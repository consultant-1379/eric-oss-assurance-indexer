/*******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.air.services;

import com.ericsson.oss.air.api.generated.model.*;
import com.ericsson.oss.air.log.LoggerHandler;
import com.ericsson.oss.air.util.Serializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Nullable;
import jakarta.json.Json;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Result;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.indices.Alias;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@EnableRetry
@Slf4j
public class OpenSearchService {

    private final OpenSearchClient client;
    private final TypeMapping typeMapping;
    @Autowired
    private LoggerHandler loggerHandler;

    public static TypeMapping getTypeMapping(String mappingInJson) {
        var bytes = mappingInJson.getBytes(StandardCharsets.UTF_8);
        var parser = Json.createParser(new ByteArrayInputStream(bytes));
        return TypeMapping._DESERIALIZER.deserialize(parser, new JacksonJsonpMapper());
    }

    @SneakyThrows
    @Autowired
    public OpenSearchService(OpenSearchClient client) {
        log.debug("Initializing SearchEngineService...");
        // TODO: externalize `open-search/mapping.json`, e.g., to application.properties?
        this.client = client;
        var mapping = StreamUtils.copyToString(new ClassPathResource("open-search/mapping.json").getInputStream(), StandardCharsets.UTF_8);
        typeMapping = getTypeMapping(mapping);
        this.loggerHandler = new LoggerHandler();
    }

    @Retryable
    @SneakyThrows
    public void createIndex (String indexName, boolean includeLifeCyclePolicy) {
        createIndex(indexName, typeMapping, includeLifeCyclePolicy);
    }

    @Retryable
    @SneakyThrows
    public boolean doesIndexExist(String indexName) {
        log.debug("Checking if index '{}' exists...", indexName);
        ExistsRequest x = new ExistsRequest.Builder().index(indexName).build();
        return client.indices().exists(x).value();
    }

    @Retryable
    @SneakyThrows
    public void createIndex(String indexName, String mapping, boolean includeLifeCyclePolicy) {
        createIndex(indexName, getTypeMapping(mapping), includeLifeCyclePolicy);
    }

    @Retryable
    @SneakyThrows
    public void createIndex(String indexName, TypeMapping typeMapping, boolean includeLifeCyclePolicy) {
        if (doesIndexExist(indexName)) {
            log.debug("Index '{}' already exists.", indexName);
            return;
        }
        log.debug("Creating index {} ...", indexName);

        CreateIndexRequest createRequest;
        if (includeLifeCyclePolicy) {
            Alias alias = new Alias.Builder()
                    .isWriteIndex(true)
                    .build();
            createRequest = new CreateIndexRequest.Builder()
                    .index(indexName + "-0") // Adding the suffix "-0" allows the rollover policy to take effect
                    .mappings(typeMapping)
                    .aliases(indexName, alias) // Adding an alias to set up the grouping of indices as they rollover
                    .build();

            createLifecyclePolicy(indexName, "1d", "3d"); // The policy can be created before the index
            createRolloverIndexTemplate(indexName);
        }
        else {
            createRequest = new CreateIndexRequest.Builder()
                    .index(indexName)
                    .mappings(typeMapping)
                    .build();
        }
        client.indices().create(createRequest);
        log.info("Index {} was created successfully", indexName);
    }

    @Retryable
    @SneakyThrows
    public void writeBulkToSearchEngine(Map<String, List<ObjectNode>> topicDocs) {
        log.debug("Writing documents to the Search Engine.");
        for (var entry : topicDocs.entrySet()) {
            var index = entry.getKey();
            var docs = entry.getValue();
            BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();

            log.debug("  - writing to index '{}' ids: [{}].", index,
                    docs.stream().map(doc -> doc.get("doc_id").asText()).collect(Collectors.joining(", ")));

            /**
             *  Disabling the Life Cycle Policy creation as it is not needed at this time.
             *  If in the future we need to re-introduce the index rollover/delete style TTL,
             *  the bool includeLifeCyclePolicy should be set to true for this call.
             */
            createIndex(index, false);

            for (var doc : docs) {
                bulkRequestBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(index)
                                .id(doc.get("doc_id").asText())
                                .document(doc)));
            }
            BulkResponse bulkResponse = client.bulk(bulkRequestBuilder.build());

            log.debug("Adding '{}' documents to index: '{}'.", docs.size(), index);

            if (bulkResponse.errors()) {
                loggerHandler.startSecurityLog();
                log.error("Some documents have not been added to the index '{}':", index);
                loggerHandler.endSecurityLog();
                for (BulkResponseItem itemResponse : bulkResponse.items()) {
                    if (itemResponse.error() != null) {
                        log.error(" - item id: '{}' with error reason: '{}'", itemResponse.id(), itemResponse.error().reason());
                    }
                }
            } else {
                log.info("All '{}' documents have been added to the index: '{}'.", docs.size(), index);
            }
        }
    }

    @Retryable
    @SneakyThrows
    public void writeDocumentToSearchEngine(Object document, String index, String id){
        log.debug("Writing one document to the Search Engine.");
        IndexRequest request = new IndexRequest.Builder()
                .id(id)
                .document(document)
                .index(index).build();
        client.index(request);

        log.info("Added a document (class:{}) with id '{}' to index '{}'. ", document.getClass().getSimpleName(), id, index);
    }

    @SneakyThrows
    public void deleteDocument(String indexName, String documentId) {
        log.debug("Deleting document '{}' from  index '{}'.", documentId, indexName);
        DeleteRequest deleteRequest = new DeleteRequest.Builder()
                .index(indexName)
                .id(documentId)
                .build();
        DeleteResponse deleteResponse = client.delete(deleteRequest);
        if (deleteResponse.result() != Result.Deleted &&
                deleteResponse.result() != Result.NotFound)
        {
            loggerHandler.startSecurityLog();
            log.error("Failed to delete document [id:{}] from index: '{}'.", documentId, indexName);
            loggerHandler.endSecurityLog();
        }
        log.info("Deleted document '{}' from  index '{}' successfully.", documentId, indexName);
    }

    @Retryable
    @SneakyThrows
    public <T> List<T> getAllDocs(String indexName, Class<T> clazz){
        log.debug("Getting all documents from index '{}'.", indexName);

        SearchRequest req = new SearchRequest.Builder()
                .index(indexName)
                .size(10000)
                .q("*")
                .build();

        return client.search(req, clazz)
                .hits()
                .hits()
                .stream().map(Hit::source)
                .collect(Collectors.toList());
    }

    @Retryable
    @SneakyThrows
    public <T> T getDocument(String indexName, String docId, Class<T> clazz) {
        log.debug("Getting document '{}' from index '{}'.", docId, indexName);
        GetRequest getRequest = new GetRequest.Builder()
                .index(indexName)
                .id(docId)
                .build();
        var getResponse = client.get(getRequest, clazz);
        if (getResponse.found()) {
            return getResponse.source();
        }
        loggerHandler.startSecurityLog();
        log.info("Document '{}' not found in index '{}'.", docId, indexName);
        loggerHandler.endSecurityLog();
        return null;
    }

    @SneakyThrows
    public List<EricOssAssuranceIndexerFieldSpec> getFields(String indexName) {
        // TODO: implement - the code below is just a stub
        log.error("This endpoint is not implemented and should not be used.");
        return new ArrayList<>();
    }

    @SneakyThrows
    public List<EricOssAssuranceIndexerFullContextSpec> getFullContexts(String indexName) {
        log.debug("Getting FullContextSpec for index '{}'...", indexName);
        FieldCapsRequest req = new FieldCapsRequest.Builder()
                .index(indexName)
                .fields("xc_*")
                .build();

        var res = client.fieldCaps(req);
        var resultList = new ArrayList<EricOssAssuranceIndexerFullContextSpec>();
        var fullContextSpec = new EricOssAssuranceIndexerFullContextSpec("full_context", new ArrayList<>());
        resultList.add(fullContextSpec);

        for( String field : res.fields().keySet() ) {
            try {
                var xContext = Serializer.decode(field.split("_")[1], EricOssAssuranceIndexerFullContext.class);
                fullContextSpec.addFullContextItem(xContext);
            } catch (Exception e) {
                log.error("Failed to decode field '{}'.", field, e);
            }
        }
        return resultList;
    }

    public List<EricOssAssuranceIndexerFullContextSpec> getFullContextsForValue(String indexName, String valueName) {
        // TODO: implement - the code below is just a stub
        log.error("This endpoint is not implemented and should not be used.");
       return new ArrayList<>();
    }

    @SneakyThrows
    public EricOssAssuranceIndexerValueContextSpec getValuesForFullContext(String indexName, String fullContextName, @Nullable String valueName) {
        log.debug("Getting ValueForFullContext for index'{}', fullContextName '{}', valueName '{}' ...", indexName, fullContextName, valueName);

        var xvPrefix = (valueName == null) ? Serializer.shortHash(fullContextName) : (Serializer.shortHash(fullContextName) + "_" + Serializer.shortHash(valueName));
        var xContextWithValuePrefix =  "xv_" + xvPrefix + "_";

        FieldCapsRequest req = new FieldCapsRequest.Builder()
                .index(indexName)
                .fields(xContextWithValuePrefix + "*")
                .build();

        var res = client.fieldCaps(req);
        var result = new EricOssAssuranceIndexerValueContextSpec(new ArrayList<>());

        for( String field : res.fields().keySet() ) {
            try {
                var parts = field.split("_");
                var encodedPart = parts[parts.length - 1];
                var xValue = Serializer.decode(encodedPart, EricOssAssuranceIndexerValueDocumentSpec.class);
                result.getValue().add(xValue);
            } catch (Exception e) {
                log.error("Failed to decode field '{}'.", field, e);
            }
        }

        return result;
    }

    /**
     * Construct and make the API call to create a policy for ISM in OpenSearch
     * to handle the time to live feature. This works by rolling over the index each day,
     * creating a new index that will act as the active "write" index. The older indices
     * will then be deleted when they have reached their time to live value.
     *
     * @param indexPatternName The base name of the index that policy should be attached to.
     *                         This corresponds to the alias name.
     *                         For example, if the actual index name is "an-index-0"
     *                         the indexPatternName is "an-index" since the "-0" will eventually
     *                         be changed when the index is rolled over.
     * @param rolloverPeriod The time before the index will be rolled over by the policy engine.
     *                       The default should be 1 day ("1d")
     * @param timeToLive The time before the index is deleted, along with all stored documents
     * @throws IOException
     */
    public void createLifecyclePolicy(String indexPatternName, String rolloverPeriod, String timeToLive) throws IOException {
        log.debug("Creating TTL Policy for index name {}.", indexPatternName);
        String policyName = indexPatternName + "--lifecycle-policy";
        String endpoint = "/_plugins/_ism/policies/" + policyName;

        Request request = new Request("PUT", endpoint);
        String policyJson = getLifeCyclePolicyJson(policyName, rolloverPeriod, timeToLive, indexPatternName);
        request.setEntity(new StringEntity(policyJson, ContentType.APPLICATION_JSON));
        Response response = ((RestClientTransport) client._transport()).restClient().performRequest(request);

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 200 && statusCode <= 299) {
            log.debug("Lifecycle policy for index " + indexPatternName + " created successfully.");
        } else {
            /* A common issue is if the policy already exists, a call to create it again will fail.
                But there is a way to update an existing policy, for example:
                localhost:9200/_plugins/_ism/policies/test--lifecycle-policy?if_seq_no=97&if_primary_term=1
             */
            HttpEntity responseEntity = response.getEntity();
            String responseJson = EntityUtils.toString(responseEntity);
            loggerHandler.startSecurityLog();
            log.error("Failed to create policy for index " + indexPatternName + ". Documents written to this index may not be cleaned up.");
            log.error("Response: " + responseJson);
            loggerHandler.endSecurityLog();
        }
    }

    /**
     * Construct and make the API call to set the rollover alias index template to all indices
     * that match the indexPatternName. This will ensure that rolled over indices are attached
     * to the correct alias.
     *
     * @param indexPatternName The base name of the index that policy should be attached to.
     *                         This corresponds to the alias name.
     *                         For example, if the actual index name is "an-index-0"
     *                         the indexPatternName is "an-index" since the "-0" will eventually
     *                         be changed when the index is rolled over.
     * @throws IOException
     */
    public void createRolloverIndexTemplate(String indexPatternName) throws IOException {
        log.debug("Creating Rollover Index Template for index name {}.", indexPatternName);
        String indexTemplateName = indexPatternName + "--rollover-template";
        String endpoint = "/_index_template/" + indexTemplateName;

        Request request = new Request("PUT", endpoint);
        String indexTemplateJson = getRolloverIndexJson(indexPatternName);
        request.setEntity(new StringEntity(indexTemplateJson, ContentType.APPLICATION_JSON));
        Response response = ((RestClientTransport) client._transport()).restClient().performRequest(request);

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 200 && statusCode <= 299) {
            log.debug("Rollover index template for index " + indexPatternName + " created successfully.");
        } else {
            HttpEntity responseEntity = response.getEntity();
            String responseJson = EntityUtils.toString(responseEntity);
            loggerHandler.startSecurityLog();
            log.error("Failed to create rollover index template for index " + indexPatternName + ". Documents written to this index will not be cleaned up.");
            log.error("Response: " + responseJson);
            loggerHandler.endSecurityLog();
        }
    }

    private String getLifeCyclePolicyJson(String policyName, String rolloverPeriod, String timeToLive, String indexPatternName) {
        return String.format ("""
                {
                  "%s": {
                    "description": "Rollover policy for indexing to implement time to live of the documents",
                    "default_state": "hot",
                    "states": [
                      {
                        "name": "hot",
                        "actions": [
                          {
                            "rollover": {
                              "min_index_age": "%s"
                            }
                          }
                        ],
                        "transitions": [
                          {
                            "state_name": "cold",
                            "conditions": {
                              "min_index_age": "%s"
                            }
                          }
                        ]
                      },
                      {
                        "name": "cold",
                        "actions": [
                          {
                            "delete": {}
                          }
                        ],
                        "transitions": []
                      }
                    ],
                    "ism_template": {
                      "index_patterns": ["%s*"]
                    }
                  }
                }""", policyName, rolloverPeriod, timeToLive, indexPatternName);
    }

    private String getRolloverIndexJson(String indexPatternName) {
        return String.format("""
                {
                  "index_patterns": ["%1$s*"],
                  "template": {
                    "settings": {
                      "plugins.index_state_management.rollover_alias": "%1$s"
                    }
                  }
                }""", indexPatternName);
    }
}
