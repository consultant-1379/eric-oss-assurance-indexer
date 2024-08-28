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

import com.ericsson.oss.air.IndexerDB;
import com.ericsson.oss.air.RecordConsumer;
import com.ericsson.oss.air.api.generated.DocumentSpecificationsApi;
import com.ericsson.oss.air.api.generated.IndexerApi;
import com.ericsson.oss.air.api.generated.IndexerListApi;
import com.ericsson.oss.air.api.generated.SearchEngineIndexListApi;
import com.ericsson.oss.air.api.generated.model.*;
import com.ericsson.oss.air.exception.HttpNotFoundException;
import com.ericsson.oss.air.services.OpenSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@RestController
public class IndexerApiImpl implements IndexerApi, DocumentSpecificationsApi, IndexerListApi, SearchEngineIndexListApi {

    @Autowired
    IndexerDB db;
    @Autowired
    RecordConsumer recordConsumer;

    @Autowired
    OpenSearchService service;

    @Override
    @RequestMapping(
            method = {RequestMethod.POST, RequestMethod.PUT},
            value = "/v1/indexer-info/indexer",
            produces = { "application/problem+json" },
            consumes = { "application/json" }
    )
    public ResponseEntity<Void> putIndexer(EricOssAssuranceIndexerIndexer indexerSpec) {
        db.addIndexer(indexerSpec);
        recordConsumer.start();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    @Override
    public ResponseEntity<EricOssAssuranceIndexerIndexer> deleteIndexer(String name) {
        var deletedIndexer = db.deleteIndexer(name);
        if (ObjectUtils.isEmpty(deletedIndexer)) {
            throw new HttpNotFoundException("An indexer with the name '" + name + "', was not found.");
        }
        recordConsumer.start();
        return new ResponseEntity<>(deletedIndexer, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<EricOssAssuranceIndexerIndexer> getIndexer(String name) {
        var indexer = db.getIndexer(name);
        if (ObjectUtils.isEmpty(indexer)) {
            throw new HttpNotFoundException("An indexer with the name '" + name + "', was not found.");
        }
        return new ResponseEntity<>(indexer, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<EricOssAssuranceIndexerFieldSpec>> getFields(String searchEngineIndexName) {
        var res = service.getFields(searchEngineIndexName);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<EricOssAssuranceIndexerFullContextSpec>> getFullContexts(String searchEngineIndexName) {
        if (!service.doesIndexExist(searchEngineIndexName)) {
            throw new HttpNotFoundException("An index with the name '" + searchEngineIndexName + "', was not found.");
        }
        var res = service.getFullContexts(searchEngineIndexName);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<EricOssAssuranceIndexerFullContextSpec>> getFullContextsForValue(String searchEngineIndexName, String valueName) {
        var res = service.getFullContextsForValue(searchEngineIndexName, valueName);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<EricOssAssuranceIndexerValueContextSpec> getValueForFullContext(String searchEngineIndexName, String valueName, String fullContextName) {
        if (!service.doesIndexExist(searchEngineIndexName)) {
            throw new HttpNotFoundException("An index with the name '" + searchEngineIndexName + "', was not found.");
        }
        var res = service.getValuesForFullContext(searchEngineIndexName, fullContextName, valueName);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<EricOssAssuranceIndexerValueContextSpec> getValuesForFullContext(String searchEngineIndexName, String fullContextName) {
        return getValueForFullContext(searchEngineIndexName, null, fullContextName);
    }


    @Override
    public ResponseEntity<List<EricOssAssuranceIndexerIndexerRef>> getIndexerList() {
        var indexerIndexerRefs = db.getIndexers().stream().map(indexer -> {
            var item = new EricOssAssuranceIndexerIndexerRef();
            item.setName(indexer.getName());
            item.setDescription(indexer.getDescription());
            return item;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(indexerIndexerRefs);
    }

    @Override
    public ResponseEntity<List<EricOssAssuranceIndexerSearchEngineIndex>> getSearchEngineIndexList() {
        var indexerSearchEngineIndices = db.getIndexers().stream()
                .map(EricOssAssuranceIndexerIndexer::getTarget)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        return ResponseEntity.ok(indexerSearchEngineIndices);
    }
}
