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

package com.ericsson.oss.air.controller;

import com.ericsson.adp.security.certm.certificatewatcher.CertificateWatcherService;
import com.ericsson.oss.air.IndexerDB;
import com.ericsson.oss.air.RecordConsumer;
import com.ericsson.oss.air.api.generated.model.EricOssAssuranceIndexerIndexer;
import com.ericsson.oss.air.exception.HttpNotFoundException;
import com.ericsson.oss.air.services.OpenSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class ApiImplTest {
    @MockBean
    private CertificateWatcherService certificateWatcherService;

    @MockBean
    private IndexerDB noIndexerDB;

    @MockBean
    private OpenSearchService noOpenSearchService;

    @Autowired
    private IndexerApiImpl api;

    ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private RecordConsumer recordConsumer;
    @Test
    public void apiTest() throws IOException {
        var indexerSpec = objectMapper.readValue(
                new File("./src/test/resources/json-files/indexer.json"), EricOssAssuranceIndexerIndexer.class);
        var aSingletonListOfIndexes = Collections.singletonList(indexerSpec);

        Mockito.when(noIndexerDB.getIndexers()).thenReturn(aSingletonListOfIndexes);
        Mockito.when(noIndexerDB.getIndexer(indexerSpec.getName())).thenReturn(indexerSpec);
        Mockito.when(noIndexerDB.getIndexer("not-available-indexer")).thenReturn(null);
        Mockito.when(noIndexerDB.deleteIndexer(indexerSpec.getName())).thenReturn(indexerSpec);
        Mockito.when(noIndexerDB.deleteIndexer("not-available-indexer")).thenReturn(null);

        var respPut = api.putIndexer(indexerSpec);
        Assertions.assertEquals(200, respPut.getStatusCode().value());

        var respGet = api.getIndexer(indexerSpec.getName());
        Assertions.assertEquals(200, respGet.getStatusCode().value());
        assertThrows(HttpNotFoundException.class, () -> api.getIndexer("not-available-indexer"));

        var respGetList = api.getIndexerList();
        Assertions.assertEquals(200, respGetList.getStatusCode().value());

        var respDelete = api.deleteIndexer(indexerSpec.getName());
        Assertions.assertEquals(200, respDelete.getStatusCode().value());
        assertThrows(HttpNotFoundException.class, () -> api.deleteIndexer("not-available-indexer"));

        var respFields = api.getFields(indexerSpec.getName());
        Assertions.assertEquals(200, respFields.getStatusCode().value());

        assertThrows(HttpNotFoundException.class, () -> api.getFullContexts("no-index-name"));

        assertThrows(HttpNotFoundException.class, () ->  api.getValuesForFullContext("no-index-name", "fullContext"));

        var respEngineIndexList = api.getSearchEngineIndexList();
        System.out.println("-----"+respEngineIndexList.getBody());

         Mockito.when(noOpenSearchService.doesIndexExist("an-index-name")).thenReturn(true);
        var respFullContexts = api.getFullContexts("an-index-name");
        Assertions.assertEquals(200, respFullContexts.getStatusCode().value());

        var respFullContextsForValue = api.getFullContextsForValue("an-index-name", "value");
        Assertions.assertEquals(200, respFullContextsForValue.getStatusCode().value());

        var respValueForFullContext = api.getValueForFullContext("an-index-name", "valueName", "fullContext");
        Assertions.assertEquals(200, respValueForFullContext.getStatusCode().value());

        var ValuesForFullContext = api.getValuesForFullContext("an-index-name", "fullContext");
        Assertions.assertEquals(200, ValuesForFullContext.getStatusCode().value());

        Assertions.assertEquals(200, respEngineIndexList.getStatusCode().value());
        recordConsumer.stop();

    }


}