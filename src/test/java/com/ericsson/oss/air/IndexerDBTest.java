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
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.DeleteResponse;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.transport.endpoints.BooleanResponse;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class IndexerDBTest {

    private OpenSearchClient openSearchClient;

    @BeforeEach
    public void setUp() throws IOException {
        openSearchClient = mock(OpenSearchClient.class);
        OpenSearchIndicesClient indicesClient = mock(OpenSearchIndicesClient.class);
        DeleteResponse deleteResponse = mock(DeleteResponse.class);
        when(deleteResponse.result()).thenReturn(null);
        when(openSearchClient.indices()).thenReturn(indicesClient);
        when(indicesClient.exists(any(ExistsRequest.class))).thenReturn(new BooleanResponse(true));
        when(openSearchClient.delete(any(DeleteRequest.class))).thenReturn(deleteResponse);
    }

    @Test
    public void deleteIndexerTest() {
        var service = Mockito.spy(new OpenSearchService(openSearchClient));
        doReturn(null).when(service).getDocument(any(),any(),any());
        IndexerDB indexerDB = new IndexerDB(service);
        try {
            indexerDB.deleteIndexer("indexer1");
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }
    @SneakyThrows
    @Test
    public void getIndexerRefsTest() {
        var service = Mockito.spy(new OpenSearchService(openSearchClient));
        IndexerDB indexerDB = new IndexerDB(service);
        doReturn(Collections.singletonList(new EricOssAssuranceIndexerIndexer())).when(service).getAllDocs(any(),any());
        try {
            indexerDB.getIndexers();
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    @Test
    public void getIndexerTest() {
        var service = Mockito.spy(new OpenSearchService(openSearchClient));
        doReturn(null).when(service).getDocument(any(),any(),any());
        IndexerDB indexerDB = new IndexerDB(service);
        try {
            indexerDB.getIndexer("indexer1");
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    @Test
    public void initInternalIndexerTest() {
        var service = Mockito.spy(new OpenSearchService(openSearchClient));
        doNothing().when(service).createIndex(any(),any(TypeMapping.class), anyBoolean());
        IndexerDB indexerDB = new IndexerDB(service);
        try {
            indexerDB.initInternalIndexIfItDoesntExist();
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    @Test
    public void verifyOrAddPrefixTest() {
        var service = Mockito.spy(new OpenSearchService(openSearchClient));
        var indexerDB = Mockito.spy(new IndexerDB(service));
        String targetIndexName_withPrefix = "assurance-an-index";
        String targetIndexName_withOutPrefix = "an-index";
        Assertions.assertEquals(targetIndexName_withPrefix, indexerDB.verifyOrAddPrefix(targetIndexName_withPrefix));
        Assertions.assertEquals(targetIndexName_withPrefix, indexerDB.verifyOrAddPrefix(targetIndexName_withOutPrefix));
    }
}
