/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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

import com.ericsson.oss.air.configuration.metrics.IndexerMetrics;
import com.ericsson.oss.air.configuration.metrics.IndexerMetricsRegistration;
import com.ericsson.oss.air.log.LoggerHandler;
import com.ericsson.oss.air.services.OpenSearchService;
import com.ericsson.oss.air.util.IndexerProcessingTimeTracker;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

import static com.ericsson.oss.air.configuration.metrics.IndexerMetrics.AIS_METRIC_TAG;

@Getter
@Component
@Slf4j
public class RecordConsumer {

    // Registry to be added to Micrometer Metrics at StartUp time with the full set of metrics
    public static final MeterRegistry registry = new SimpleMeterRegistry();
    public MeterRegistry getMeterRegistry() { return registry; }

    private final IndexerProcessingTimeTracker indexerProcessingTimeTracker = new IndexerProcessingTimeTracker();

    private final IndexerMetricsRegistration metricsRegister = new IndexerMetricsRegistration(registry);

    private static final Counter recordsReceivedCounter = IndexerMetricsRegistration.registerRecordsReceivedCount(registry);

    private static final Counter recordsProcessedCounter = IndexerMetricsRegistration.registerRecordsProcessedCount(registry);

    // ---- Following are only for testing metric searches in the registry ----
    Search receivedRecordsMetricDescriptorName;
    Search processedRecordsMetricDescriptorName;

    /**
     * Search for a metric in the MetricRegister using the input search arguments composed of a name and a tag.
     * Note: If the metric is not found (registered) Null is returned.
     * @return Counter or null if not found
     */
    @Nullable
    Counter getRegisteredMetricCounter(Search metricDescriptionSearchName) {
        return metricDescriptionSearchName.counter();
    }
    // ------------------------------------------------------------------------
    public void consumerAction(OpenSearchService service, KafkaConsumer<String, GenericRecord> consumer,
                                      ImmutableMap<String, Map<String, List<Writer>>> router) throws IOException {
        ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofMillis(2000));

        if (!records.isEmpty()) {
            log.info("Received {} records", records.count());

            recordsReceivedCounter.increment(records.count());
            Counter globalRegistryReceivedCounter = Metrics.counter(IndexerMetrics.RECORDS_RECEIVED_COUNT.getMetricName(),
                AIS_METRIC_TAG, IndexerMetrics.RECORDS_RECEIVED_COUNT.getMetricName());
            log.debug("records-received-count: {}",
                Objects.requireNonNull(getRegisteredMetricCounter(getReceivedRecordsMetricDescriptorName())).count());
            log.debug("Micrometer.Metrics::globalRegistry received-records-count: {}", globalRegistryReceivedCounter.count());

            service.writeBulkToSearchEngine(processBatch(records, router));
            consumer.commitAsync();

            recordsProcessedCounter.increment(records.count());
            Counter globalRegistryProcessedCounter = Metrics.counter(IndexerMetrics.RECORDS_PROCESSED_COUNT.getMetricName(),
                AIS_METRIC_TAG, IndexerMetrics.RECORDS_PROCESSED_COUNT.getMetricName());
            log.debug("records-processed-count: {}",
                Objects.requireNonNull(getRegisteredMetricCounter(getProcessedRecordsMetricDescriptorName())).count());
            log.debug("Micrometer.Metrics::globalRegistry processed-records-count: {} ", globalRegistryProcessedCounter.count());
        }
    }

    @Autowired
    private IndexerDB indexerDB;
    @Autowired
    private OpenSearchService service;
    String groupId;
    @Autowired
    private Properties consumerProps;
    private RunningThread runningThread;
    @Autowired
    private LoggerHandler loggerHandler;
    private final class RunningThread {
        private final Thread thread;
        private RunningThread() {
            log.debug("Starting RecordConsumer thread '{}'...", groupId);
            thread = new Thread(() -> {
                try (KafkaConsumer<String, GenericRecord> consumer = new KafkaConsumer<>(consumerProps)) {
                    consumerLoop(consumer);
                }
            });
            thread.start();
        }
        Thread getThread() {
            return thread;
        }

        @SneakyThrows
        private void consumerLoop(KafkaConsumer<String, GenericRecord> consumer) {
            log.info("RecordConsumer thread loop '{}' is running...", groupId);
            ImmutableSet<String> topics = ImmutableSet.of();
            while (this == runningThread) {
                var router = indexerDB.getRouter();
                var newTopics= router.keySet();
                if (newTopics.isEmpty()) {
                    loggerHandler.startSecurityLog();
                    log.info("RecordConsumer, group-id='{}', has no topics to subscribe to. Will retry in 5 seconds...", groupId);
                    loggerHandler.endSecurityLog();
                    Thread.sleep(5000);
                    continue;
                }
                if (!topics.equals(newTopics)) {
                    log.info("RecordConsumer, group-id='{}', will subscribe to new topics: old:{} -> new:{}", groupId, topics, newTopics);
                    topics = newTopics;
                    consumer.subscribe(topics);
                }
                consumerAction(service, consumer, router);
            }
            loggerHandler.startSecurityLog();
            log.info("RecordConsumer '{}' has gracefully stopped.", groupId);
            loggerHandler.endSecurityLog();
        }
    }

    /**
     * For testing purpose only.
     */
    @SneakyThrows
    public final synchronized void stopAndWait() {
        log.info("This function is to be used in tests only!");
        if (this.runningThread != null) {
            var thread = this.runningThread.getThread();
            stop();
            thread.join();
        }
    }
    public final synchronized void stop() {
        loggerHandler.startSecurityLog();
        log.debug("Stopping RecordConsumer '{}'...", groupId);
        loggerHandler.endSecurityLog();
        this.runningThread = null;
    }
    public final synchronized void start() {
        loggerHandler.startSecurityLog();
        log.info("Starting RecordConsumer '{}'...", groupId);
        loggerHandler.endSecurityLog();
        this.runningThread = new RunningThread();
    }

    public final synchronized boolean isRunning() {
        return (this.runningThread != null && this.runningThread.getThread().isAlive());
    }

    public RecordConsumer(){
        initMetrics();
    }

    private void initMetrics () {
        metricsRegister.registerIndexerProcessingTime(indexerProcessingTimeTracker);

        // Add the registry to Micrometer Metrics so it is in the CompositeMeterRegistry (globalRegistry),
        // so Prometheus can poll (scrape) them periodically.
        Metrics.addRegistry(registry);
        // Save the Metric Search Criteria for the Counter metrics to be used when retrieving them
        // from the MetricRegistry
        receivedRecordsMetricDescriptorName = registry.find(IndexerMetrics.RECORDS_RECEIVED_COUNT.getMetricName())
            .tag(AIS_METRIC_TAG, IndexerMetrics.RECORDS_RECEIVED_COUNT.getMetricName());
        processedRecordsMetricDescriptorName = registry.find(IndexerMetrics.RECORDS_PROCESSED_COUNT.getMetricName())
            .tag(AIS_METRIC_TAG, IndexerMetrics.RECORDS_PROCESSED_COUNT.getMetricName());

    }
    public RecordConsumer(Properties consumerProps, IndexerDB indexerDB, OpenSearchService service) {
        log.info("Creating RecordConsumer with explicitly provided properties ...");
        this.consumerProps = new Properties();
        this.consumerProps.putAll(consumerProps);
        this.indexerDB = indexerDB;
        this.service = service;
        this.loggerHandler = new LoggerHandler();
        initMetrics();

    }
    @Order(3)
    @EventListener(ApplicationReadyEvent.class)
    public void doAfterStartUp() {
        Long startTime = System.currentTimeMillis();
        indexerProcessingTimeTracker.setAisProcessingStartTime(startTime);

        log.info("Creating RecordConsumer with properties: {}", consumerProps);
        this.groupId = consumerProps.getProperty(ConsumerConfig.GROUP_ID_CONFIG);

        start();

        Long finishTime = System.currentTimeMillis();
        indexerProcessingTimeTracker.setAisProcessingFinishTime(finishTime);
        log.info("Time to start-up AIS: {} sec", indexerProcessingTimeTracker.getAisProcessingTime());

        Counter globalRegistryReceivedCounter = Metrics.counter(IndexerMetrics.RECORDS_RECEIVED_COUNT.getMetricName(),
            AIS_METRIC_TAG, IndexerMetrics.RECORDS_RECEIVED_COUNT.getMetricName());
        log.info("initial records_received_count: {}",
            Objects.requireNonNull(getRegisteredMetricCounter(getReceivedRecordsMetricDescriptorName())).count());
        log.info("Micrometer.Metrics::globalRegistry received-records-count: {}", globalRegistryReceivedCounter.count());

        Counter globalRegistryProcessedCounter = Metrics.counter(IndexerMetrics.RECORDS_PROCESSED_COUNT.getMetricName(),
            AIS_METRIC_TAG, IndexerMetrics.RECORDS_PROCESSED_COUNT.getMetricName());
        log.info("initial records_processed_count: {}",
            Objects.requireNonNull(getRegisteredMetricCounter(getProcessedRecordsMetricDescriptorName())).count());
        log.info("Micrometer.Metrics::globalRegistry processed-records-count: {} ", globalRegistryProcessedCounter.count());

        log.info("RecordConsumer '{}' is ready! ", groupId);
    }

    public static Map<String, List<ObjectNode>> processBatch( ConsumerRecords<String, GenericRecord> records , final Map<String, Map<String, List<Writer>>> router) {
        log.info("Processing a BATCH of {} records...", records.count());
        var documentsPerIndex = new HashMap<String, List<ObjectNode>>();
        for (ConsumerRecord<String, GenericRecord> record : records) {
            var topic = record.topic();
            var theRecord = record.value();
            var theSchemaName = theRecord.getSchema().getFullName();
            var writers = router
                    .getOrDefault(topic,Collections.emptyMap())
                    .getOrDefault(theSchemaName,Collections.emptyList());
            if (writers.isEmpty()) {
                // TODO: do not flood the log! metrics?
                //       Record a dropped Record count for No Writer here?
                log.info("WARNING: No writer for topic/schema: '{}/{}'!", topic, theSchemaName);
            }
            for (Writer writer : writers) {
                var indexName = writer.getIndexName();
                var docs = writer.recordToDocuments(theRecord);
                for (var doc : docs) {
                    doc.set("offset", new TextNode(Long.toString(record.offset())));
                    doc.set("vintage", new TextNode(writer.vintage));
                    documentsPerIndex.computeIfAbsent(indexName, k -> new ArrayList<>()).add(doc);
                }
            }
        }
        log.debug("BATCH RESULT: {}", documentsPerIndex);
        return documentsPerIndex;
    }
}
