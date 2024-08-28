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

package com.ericsson.oss.air;

import com.ericsson.oss.air.api.generated.model.EricOssAssuranceIndexerIndexer;
import com.ericsson.oss.air.services.OpenSearchService;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class IndexerDB {

    // The prefix to be added to all indices created in OpenSearch.
    // This must follow the prefix naming used in the "modificationAllowedIndices" for SearchEngine in the BAM platform
    // This should eventually be moved to a configuration file like application.yaml
    private static final String INDEX_PREFIX_NAME = "assurance";

    // The name of the internal index used by Indexer to track the indexer specs
    // This should eventually be moved to a configuration file like application.yaml
    private static final String INTERNAL_INDEX_NAME = "assurance-internal-index";

    @Autowired
    private final OpenSearchService service;

    @Autowired
    public IndexerDB(OpenSearchService service) {
        this.service = service;
    }

    @SneakyThrows
    @Order(2)
    @EventListener(ApplicationReadyEvent.class)
    public void initInternalIndexIfItDoesntExist() {
        try {
            log.info("Initializing internal index '{}'...", INTERNAL_INDEX_NAME);
            var internalIndexMapping = StreamUtils.copyToString(
                new ClassPathResource("open-search/internal-index-mapping.json").getInputStream(),
                StandardCharsets.UTF_8);
            service.createIndex(INTERNAL_INDEX_NAME, internalIndexMapping, false);
        } catch (Exception e) {
            log.error("Failed to initialize internal index '{}'", INTERNAL_INDEX_NAME, e);
        }
    }

    public synchronized ImmutableMap<String, Map<String,List<Writer>>> getRouter() {
        log.debug("Returning router...");
        List<EricOssAssuranceIndexerIndexer> indexers = service.getAllDocs(INTERNAL_INDEX_NAME, EricOssAssuranceIndexerIndexer.class);
        Map<String, Map<String,List<Writer>>> newRouter = new HashMap<>();
        for (var indexer: indexers) {
            // TODO: We still do not know the topic!!!
            String topic = indexer.getSource().getName();
            Map<String, List<Writer>> dispatch = newRouter.getOrDefault(topic, new HashMap<>());
            for (int i =0, len = indexer.getWriters().size(); i < len; i++) {
                var w = new Writer(indexer, i);
                var schemaName = w.getSchemaName();
                dispatch.computeIfAbsent(schemaName, k -> new ArrayList<>()).add(w);
            }
            newRouter.put(topic, dispatch);
        }
        return ImmutableMap.copyOf(newRouter);
    }

    public synchronized List<EricOssAssuranceIndexerIndexer> getIndexers(){
        log.debug("Returning list of indexers...");
        return service.getAllDocs(INTERNAL_INDEX_NAME, EricOssAssuranceIndexerIndexer.class);
    }

    /**
     * add or update an indexer (the update is persisted)
     *
     * @param indexer - as defined by openAPI
     */
    public synchronized void addIndexer(EricOssAssuranceIndexerIndexer indexer) {
        var indexerName = indexer.getName();
        var targetName = verifyOrAddPrefix(indexer.getTarget().getName());
        // TODO: This is the place we hack around the index name - we should not alter the user input
        indexer.getTarget().name(targetName);
        service.createIndex(targetName, false);
        log.debug("Adding indexer '{}'...", indexerName);
        service.writeDocumentToSearchEngine(indexer, INTERNAL_INDEX_NAME, indexerName);
    }

    public synchronized EricOssAssuranceIndexerIndexer deleteIndexer(String indexerName) {
        log.debug("Deleting indexer '{}'...", indexerName);
        var deletedIndexer = service.getDocument(INTERNAL_INDEX_NAME, indexerName, EricOssAssuranceIndexerIndexer.class);
        service.deleteDocument(INTERNAL_INDEX_NAME,indexerName);
        return deletedIndexer;
    }

    public synchronized EricOssAssuranceIndexerIndexer getIndexer(String indexerName) {
        log.debug("Getting indexer '{}'...", indexerName);
        return service.getDocument(INTERNAL_INDEX_NAME, indexerName, EricOssAssuranceIndexerIndexer.class);
    }

    public String verifyOrAddPrefix(String targetIndexName) {
        // If the expected prefix is missing, then add the prefix to the index name
        if (!targetIndexName.startsWith(INDEX_PREFIX_NAME)) {
            log.info("WARNING: Target index name '{}' is missing the prefix '{}'. Adding it by default", targetIndexName, INDEX_PREFIX_NAME);
            return INDEX_PREFIX_NAME + "-" + targetIndexName;
        }
        return targetIndexName;
    }

}
