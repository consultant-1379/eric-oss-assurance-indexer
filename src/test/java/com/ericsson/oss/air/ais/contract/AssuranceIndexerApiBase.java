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

package com.ericsson.oss.air.ais.contract;

/**
 * Base class for AI API contract tests.
 */

import com.ericsson.adp.security.certm.certificatewatcher.CertificateWatcherService;
import com.ericsson.oss.air.api.generated.model.*;
import com.ericsson.oss.air.exception.HttpNotFoundException;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;
import com.ericsson.oss.air.IndexerDB;
import com.ericsson.oss.air.controller.IndexerApiImpl;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static com.ericsson.oss.air.ais.contract.IndexTestUtil.*;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest
public class AssuranceIndexerApiBase {
    @MockBean
    private CertificateWatcherService certificateWatcherService;

    @MockBean
    private IndexerDB noIndexerDB;

    @MockBean
    private IndexerApiImpl indexerApiImpl;

    private AutoCloseable closeable;

    @Autowired
    protected WebApplicationContext webApplicationContext;

    // This is called to set up the stub mocker for the contract tests
    @BeforeEach
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        RestAssuredMockMvc.webAppContextSetup(this.webApplicationContext);

        // GET /v1/indexer-info/indexer
        when(indexerApiImpl.getIndexer(INDEX_NAME)).thenReturn(new ResponseEntity<EricOssAssuranceIndexerIndexer>(INDEX_RESPONSE, HttpStatus.OK));
        when(indexerApiImpl.getIndexer(INVALID_INDEX_NAME)).thenThrow(new HttpNotFoundException("An indexer with the name 'invalidIndexName', was not found."));

        // DELETE /v1/indexer-info/indexer
        when(indexerApiImpl.deleteIndexer(INDEX_NAME)).thenReturn(new ResponseEntity<EricOssAssuranceIndexerIndexer>(INDEX_RESPONSE, HttpStatus.OK));
        when(indexerApiImpl.deleteIndexer(INVALID_INDEX_NAME)).thenThrow(new HttpNotFoundException("An indexer with the name 'invalidIndexName', was not found."));

        // GET /v1/indexer-info/indexer-list
        List<EricOssAssuranceIndexerIndexerRef> indexRefs = new ArrayList<EricOssAssuranceIndexerIndexerRef>();
        indexRefs.add(INDEX_REF_A);
        indexRefs.add(INDEX_REF_B);
        when(indexerApiImpl.getIndexerList()).thenReturn(new ResponseEntity<List<EricOssAssuranceIndexerIndexerRef>>(indexRefs, HttpStatus.OK));

        // GET /v1/indexer-info/search-engine-index-list
        List<EricOssAssuranceIndexerSearchEngineIndex> searchEngineIndex = new ArrayList<EricOssAssuranceIndexerSearchEngineIndex>();
        searchEngineIndex.add(SEARCH_ENGINE_LIST_RESPONSE);
        when(indexerApiImpl.getSearchEngineIndexList()).thenReturn(new ResponseEntity<List<EricOssAssuranceIndexerSearchEngineIndex>>(searchEngineIndex, HttpStatus.OK));

        // GET /v1/indexer-info/spec/fullcontexts
        List<EricOssAssuranceIndexerFullContextSpec> fullContextSpecs = new ArrayList<EricOssAssuranceIndexerFullContextSpec>();
        fullContextSpecs.add(FULL_CONTEXT_RESPONSE);
        when(indexerApiImpl.getFullContexts(SEARCH_ENGINE_INDEX)).thenReturn(new ResponseEntity<List<EricOssAssuranceIndexerFullContextSpec>>(fullContextSpecs, HttpStatus.OK));
        when(indexerApiImpl.getFullContexts(INVALID_SEARCH_ENGINE_INDEX)).thenThrow(new HttpNotFoundException("An item with the search engine index 'invalidSearchEngineIndexName', was not found."));

        // GET /v1/indexer-info/spec/values-for-fullcontext
        when(indexerApiImpl.getValuesForFullContext(SEARCH_ENGINE_INDEX, FULL_CONTEXT_NAME_A)).thenReturn(new ResponseEntity<EricOssAssuranceIndexerValueContextSpec>(FULL_CONTEXT_VALUES_RESPONSE, HttpStatus.OK));
        when(indexerApiImpl.getValuesForFullContext(INVALID_SEARCH_ENGINE_INDEX, FULL_CONTEXT_NAME_A)).thenThrow(new HttpNotFoundException("An item with the search engine index 'invalidSearchEngineIndexName', was not found."));
        when(indexerApiImpl.getValuesForFullContext(SEARCH_ENGINE_INDEX, INVALID_FULL_CONTEXT_NAME)).thenThrow(new HttpNotFoundException("An item with the context index 'invalidContext', was not found."));
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }
}