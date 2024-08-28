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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ericsson.oss.air.api.generated.model.EricOssAssuranceIndexerContextFieldSpec;
import com.ericsson.oss.air.api.generated.model.EricOssAssuranceIndexerFullContext;
import com.ericsson.oss.air.api.generated.model.EricOssAssuranceIndexerValueDocumentSpec;
import com.ericsson.oss.air.util.Serializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.RestClient;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.search.HitsMetadata;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenSearchServiceTest {
    ObjectMapper objectMapper = new ObjectMapper();
    private OpenSearchClient openSearchClient;
    private OpenSearchIndicesClient indicesClient;
    private RestClientTransport clientTransport;
    private RestClient restClient;
    private final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    private final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    public static final String FACILITY_KEY = "facility";
    public static final String SUBJECT_KEY = "subject";
    public static final String AUDIT_LOG = "log audit";

    @BeforeEach
    public void setUp() throws IOException {
        openSearchClient = mock(OpenSearchClient.class);
        indicesClient = mock(OpenSearchIndicesClient.class);
        when(openSearchClient.indices()).thenReturn(indicesClient);
        when(indicesClient.exists(any(ExistsRequest.class))).thenReturn(new BooleanResponse(false));
        logger.addAppender(listAppender);
        listAppender.start();
    }

    @AfterEach
    void cleanup() {
        listAppender.stop();
        listAppender.list.clear();
        logger.detachAppender(listAppender);
    }

    @Test
    void createIndexTest() throws IOException {
        var service = new OpenSearchService(openSearchClient);
        service.createIndex("index-does-not-exist", "{}", false);
        when(indicesClient.exists(any(ExistsRequest.class))).thenReturn(new BooleanResponse(true));
        service.createIndex("index-exists", false);
    }

    @Test
    void createIndexWithTTLTest() throws IOException {
        clientTransport = mock(RestClientTransport.class);
        restClient = mock(RestClient.class);
        when(openSearchClient._transport()).thenReturn(clientTransport);
        when(clientTransport.restClient()).thenReturn(restClient);
        Response mockResponse = mock(Response.class);
        when(restClient.performRequest(any(Request.class))).thenReturn(mockResponse);
        StatusLine mockOkStatusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "Ok");
        when(mockResponse.getStatusLine()).thenReturn(mockOkStatusLine);

        var service = new OpenSearchService(openSearchClient);
        service.createIndex("index-with-ttl-does-not-exist", "{}", true);
        when(indicesClient.exists(any(ExistsRequest.class))).thenReturn(new BooleanResponse(true));
        service.createIndex("index-with-ttl-exists", true);

    }

    @SneakyThrows
    @Test
    void createDuplicateTTLPolicyTest() {
        clientTransport = mock(RestClientTransport.class);
        restClient = mock(RestClient.class);
        when(openSearchClient._transport()).thenReturn(clientTransport);
        when(clientTransport.restClient()).thenReturn(restClient);
        Response mockResponse = mock(Response.class);
        when(restClient.performRequest(any(Request.class))).thenReturn(mockResponse);
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream("Duplicate Policy.".getBytes()));
        when(mockResponse.getEntity()).thenReturn(entity);
        StatusLine mockOkStatusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 400, "Bad Request");
        when(mockResponse.getStatusLine()).thenReturn(mockOkStatusLine);

        var service = new OpenSearchService(openSearchClient);
        service.createLifecyclePolicy("index-with-ttl-exists", "5m", "30m");
        service.createRolloverIndexTemplate("index-with-ttl-exists");
    }

    @SneakyThrows
    @Test
    void createDuplicateTTLPolicySecurityLogTest() {
        clientTransport = mock(RestClientTransport.class);
        restClient = mock(RestClient.class);
        when(openSearchClient._transport()).thenReturn(clientTransport);
        when(clientTransport.restClient()).thenReturn(restClient);
        Response mockResponse = mock(Response.class);
        when(restClient.performRequest(any(Request.class))).thenReturn(mockResponse);
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream("Duplicate Policy.".getBytes()));
        when(mockResponse.getEntity()).thenReturn(entity);
        StatusLine mockOkStatusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 400, "Bad Request");
        when(mockResponse.getStatusLine()).thenReturn(mockOkStatusLine);

        var service = new OpenSearchService(openSearchClient);
        service.createLifecyclePolicy("index-with-ttl-exists", "5m", "30m");
        service.createRolloverIndexTemplate("index-with-ttl-exists");

        /// Number of secureLogs events occurrences
        var secureLogs = listAppender.list.stream()
                .filter(entry -> entry.getMDCPropertyMap().containsKey(FACILITY_KEY) && entry.getMDCPropertyMap().get(FACILITY_KEY).equals(AUDIT_LOG))
                .filter(entry -> entry.getMDCPropertyMap().containsKey(SUBJECT_KEY) && entry.getMDCPropertyMap().get(SUBJECT_KEY).equals("ASSURANCE-INDEXER"));
        assertEquals(4, secureLogs.count());

    }

    @Test
    void writeBulkToSearchEngineTest() throws IOException {
        var service = new OpenSearchService(openSearchClient);
        var resp = mock(BulkResponse.class);
        when(resp.errors()).thenReturn(false);
        when(openSearchClient.bulk(any(BulkRequest.class))).thenReturn(resp);

        var doc = objectMapper.createObjectNode();
        doc.put("doc_id", "a-doc-id");
        service.writeBulkToSearchEngine(Collections.singletonMap("index", Collections.singletonList(doc)));
    }

    @Test
    void writeBulkToSearchEngineSecurityLogTest() throws IOException {
        var service = new OpenSearchService(openSearchClient);

        /// Bulk Response has errors
        var resp_error = mock(BulkResponse.class);
        when(resp_error.errors()).thenReturn(true);
        when(openSearchClient.bulk(any(BulkRequest.class))).thenReturn(resp_error);

        var doc_error = objectMapper.createObjectNode();
        doc_error.put("doc_id", "a-doc-id");
        service.writeBulkToSearchEngine(Collections.singletonMap("index", Collections.singletonList(doc_error)));

        /// Number of secureLogs events occurrences
        var secureLogs = listAppender.list.stream()
                .filter(entry -> entry.getMDCPropertyMap().containsKey(FACILITY_KEY) && entry.getMDCPropertyMap().get(FACILITY_KEY).equals(AUDIT_LOG))
                .filter(entry -> entry.getMDCPropertyMap().containsKey(SUBJECT_KEY) && entry.getMDCPropertyMap().get(SUBJECT_KEY).equals("ASSURANCE-INDEXER"));
        assertEquals(1, secureLogs.count());

    }

    @SneakyThrows
    @Test
    void writeDocumentToSearchEngineTest() {
        var service = new OpenSearchService(openSearchClient);
        service.createIndex("index", false);
        service.createIndex("index", StreamUtils.copyToString(new ClassPathResource("open-search/internal-index-mapping.json").getInputStream(), StandardCharsets.UTF_8), false);
    }

    @SneakyThrows
    @Test
    void deleteDocumentTest() {
        var service = new OpenSearchService(openSearchClient);
        var resp = mock(DeleteResponse.class);
        when(resp.result()).thenReturn(null);
        when(openSearchClient.delete(any(DeleteRequest.class))).thenReturn(resp);
        service.deleteDocument("index", "id");
    }

    @SneakyThrows
    @Test
    void deleteDocumentSecurityLogTest() {
        var service = new OpenSearchService(openSearchClient);
        var resp = mock(DeleteResponse.class);
        when(resp.result()).thenReturn(null);
        when(openSearchClient.delete(any(DeleteRequest.class))).thenReturn(resp);
        service.deleteDocument("index", "id");

        /// Number of secureLogs occurrences
        var secureLogs = listAppender.list.stream()
                .filter(entry -> entry.getMDCPropertyMap().containsKey(FACILITY_KEY) && entry.getMDCPropertyMap().get(FACILITY_KEY).equals(AUDIT_LOG))
                .filter(entry -> entry.getMDCPropertyMap().containsKey(SUBJECT_KEY) && entry.getMDCPropertyMap().get(SUBJECT_KEY).equals("ASSURANCE-INDEXER"));
        assertEquals(1, secureLogs.count());
    }

    @SneakyThrows
    @Test
    void getAllDocsTest() {
        var service = new OpenSearchService(openSearchClient);
        var resp = mock(SearchResponse.class);
        var hits = mock(HitsMetadata.class);
        when(hits.hits()).thenReturn(Collections.emptyList());
        when(resp.hits()).thenReturn(hits);
        when(openSearchClient.search(any(SearchRequest.class), any())).thenReturn(resp);
        service.getAllDocs("index", Object.class);
    }
    @SneakyThrows
    @Test
    void getDocumentTest() {
        var service = new OpenSearchService(openSearchClient);
        var resp = mock(GetResponse.class);
        when(resp.source()).thenReturn(null);
        when(openSearchClient.get(any(GetRequest.class), any())).thenReturn(resp);
        service.getDocument("index", "id-not-found", Object.class);
        when(resp.found()).thenReturn(true);
        service.getDocument("index", "id", Object.class);
    }

    @SneakyThrows
    @Test
    void getDocumentSecurityLogTest() {
        var service = new OpenSearchService(openSearchClient);
        var resp = mock(GetResponse.class);
        when(resp.source()).thenReturn(null);
        when(openSearchClient.get(any(GetRequest.class), any())).thenReturn(resp);
        service.getDocument("index", "id-not-found", Object.class);

        /// Number of secureLogs occurrences
        var secureLogs = listAppender.list.stream()
                .filter(entry -> entry.getMDCPropertyMap().containsKey(FACILITY_KEY) && entry.getMDCPropertyMap().get(FACILITY_KEY).equals(AUDIT_LOG))
                .filter(entry -> entry.getMDCPropertyMap().containsKey(SUBJECT_KEY) && entry.getMDCPropertyMap().get(SUBJECT_KEY).equals("ASSURANCE-INDEXER"));
        assertEquals(1, secureLogs.count());
    }

    @SneakyThrows
    @Test
    void getFieldsTest() {
        var service = new OpenSearchService(openSearchClient);
        service.getFields("index");
    }

    @SneakyThrows
    @Test
    void getFullContextsTest() {
        var service = new OpenSearchService(openSearchClient);
        var resp = mock(FieldCapsResponse.class);

        when(openSearchClient.fieldCaps(any(FieldCapsRequest.class))).thenReturn(resp);

        var context = "context1_context2";
        var contextFields = context.split("_");
        var xContext = new EricOssAssuranceIndexerFullContext(context, new ArrayList<>());
        xContext.getContext().add(new EricOssAssuranceIndexerContextFieldSpec(contextFields[0], "c_"+ contextFields[0]));
        xContext.getContext().add(new EricOssAssuranceIndexerContextFieldSpec(contextFields[1], "c_"+ contextFields[1]));
        var xFieldName = "xc_" + Serializer.encode(xContext);

        when(resp.fields()).thenReturn(Collections.singletonMap(xFieldName,Collections.emptyMap()));
        var fullContexts = service.getFullContexts("index");

        System.out.println(xFieldName);
        System.out.println(fullContexts);
    }

    @SneakyThrows
    @Test
    void getFullContextsBrokenFieldTest() {
        var service = new OpenSearchService(openSearchClient);
        var resp = mock(FieldCapsResponse.class);
        when(openSearchClient.fieldCaps(any(FieldCapsRequest.class))).thenReturn(resp);
        when(resp.fields()).thenReturn(Collections.singletonMap("xc_BROKEN01234",Collections.emptyMap()));
        var fullContexts = service.getFullContexts("index");
        assertEquals(0, fullContexts.get(0).getFullContext().size());
    }

    @SneakyThrows
    @Test
    void getFullContextsForValueTest() {
        var service = new OpenSearchService(openSearchClient);
        service.getFullContextsForValue("index", "value");
    }

    @SneakyThrows
    @Test
    void getValueForFullContextTest() {
        var service = new OpenSearchService(openSearchClient);
        var resp = mock(FieldCapsResponse.class);
        when(openSearchClient.fieldCaps(any(FieldCapsRequest.class))).thenReturn(resp);
        var xValue = new EricOssAssuranceIndexerValueDocumentSpec("v0", "valueContextDocumentName", "valueDocumentName");
        var xFieldName = "xv_" + Serializer.encode(xValue);
        when(resp.fields()).thenReturn(Collections.singletonMap(xFieldName,Collections.emptyMap()));
        var valueContextSpec = service.getValuesForFullContext("index", "fullContext", "valueName");
        assertEquals(xValue, valueContextSpec.getValue().get(0));
    }

    @SneakyThrows
    @Test
    void getValueForFullContextBrokenTest() {
        var service = new OpenSearchService(openSearchClient);
        var resp = mock(FieldCapsResponse.class);
        when(openSearchClient.fieldCaps(any(FieldCapsRequest.class))).thenReturn(resp);
        when(resp.fields()).thenReturn(Collections.singletonMap("xv_BROKEN_BROKEN_1234",Collections.emptyMap()));
        var valueContextSpec = service.getValuesForFullContext("index", "fullContext", "valueName");
        assertEquals(0, valueContextSpec.getValue().size());
    }
}