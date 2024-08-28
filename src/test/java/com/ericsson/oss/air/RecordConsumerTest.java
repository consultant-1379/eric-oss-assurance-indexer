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
import com.ericsson.oss.air.configuration.metrics.IndexerMetrics;
import com.ericsson.oss.air.services.OpenSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.search.Search;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.transport.endpoints.BooleanResponse;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.ericsson.oss.air.configuration.metrics.IndexerMetrics.AIS_METRIC_TAG;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecordConsumerTest {
    private OpenSearchClient openSearchClient;

    @BeforeEach
    public void setUp() throws IOException {
        openSearchClient = mock(OpenSearchClient.class);
        var indicesClient = mock(OpenSearchIndicesClient.class);
        when(openSearchClient.indices()).thenReturn(indicesClient);
        when(indicesClient.exists(any(ExistsRequest.class))).thenReturn(new BooleanResponse(true));

    }

    private final Properties consumerProps = new Properties() {
        {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
            put(ConsumerConfig.GROUP_ID_CONFIG, "test");
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroDeserializer");
            put("schema.registry.url", "http://localhost:8081");
        }
    };

    @Test
    void createStartAndStopTest() {
        var service = new OpenSearchService(openSearchClient);
        var indexerDB = new IndexerDB(service);
        try {
            RecordConsumer recordConsumer = new RecordConsumer(consumerProps, indexerDB, service);
            recordConsumer.start();
            recordConsumer.stopAndWait();
            recordConsumer.start();
            recordConsumer.start();
            recordConsumer.stopAndWait();
            recordConsumer.stop();
            recordConsumer.stop();
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    @Test
    void startUpTimeTest() {
        try {
            var service = new OpenSearchService(openSearchClient);
            var indexerDB = new IndexerDB(service);
            RecordConsumer recordConsumer = new RecordConsumer(consumerProps, indexerDB, service);

            recordConsumer.start();

            final var indexerProcessingTimeTracker = recordConsumer.getIndexerProcessingTimeTracker();

            recordConsumer.doAfterStartUp();

            double doAfterStartUpTime = indexerProcessingTimeTracker.getAisProcessingTime();
            Assertions.assertTrue(doAfterStartUpTime >= 0.0);

            Counter micrometerRegistryCounter = Metrics.counter(IndexerMetrics.RECORDS_RECEIVED_COUNT.getMetricName(),
                AIS_METRIC_TAG, IndexerMetrics.RECORDS_RECEIVED_COUNT.getMetricName());
            Counter recConsumerRegCounter = RecordConsumer.registry.find(IndexerMetrics.RECORDS_RECEIVED_COUNT.getMetricName()).counter();

            assertEquals(micrometerRegistryCounter.getId().getName(), recConsumerRegCounter.getId().getName());
            assertTrue(micrometerRegistryCounter.count() >=  0.0);

            micrometerRegistryCounter = Metrics.counter(IndexerMetrics.RECORDS_PROCESSED_COUNT.getMetricName(),
                AIS_METRIC_TAG, IndexerMetrics.RECORDS_PROCESSED_COUNT.getMetricName());
            recConsumerRegCounter = RecordConsumer.registry.find(IndexerMetrics.RECORDS_PROCESSED_COUNT.getMetricName()).counter();

            assertEquals(micrometerRegistryCounter.getId().getName(), recConsumerRegCounter.getId().getName());
            assertTrue(micrometerRegistryCounter.count() >= 0.0);

            double startTimeFromTracker = indexerProcessingTimeTracker.getAisProcessingTime();
            Assertions.assertTrue(startTimeFromTracker >= 0.0);

        } catch (Exception e) {
            Assertions.fail(e);
        }

    }

    @Test
    void processBatchTest() throws IOException {
        var mapper = new ObjectMapper();
        var service = Mockito.spy(new OpenSearchService(openSearchClient));
        IndexerDB indexerDB = new IndexerDB(service);
        var openApiIndexer = mapper.readValue(
            new File("./src/test/resources/json-files/indexer.json"),
            EricOssAssuranceIndexerIndexer.class
        );
        var topicName = openApiIndexer.getSource().getName();

        List<ConsumerRecord<String, GenericRecord>> records = new ArrayList<>();

        var schema1 = new Schema.Parser().parse(new File("./src/test/resources/avro-schemas/schema1.avsc"));
        var record = new GenericRecordBuilder(schema1)
                .set("c1", "valueForC1")
                .set("c2", "CX:valueForC2")
                .set("rvi", 1684322409967L)
                .set("rvf", 0.6)
                .set("ri1", "info 1: ?")
                .set("ri2", "info 2:? ")
                .build();

        records.add(new ConsumerRecord<String,GenericRecord >(topicName, 0, 0, null, record));

        var emptyDocs = RecordConsumer.processBatch(
                new ConsumerRecords<>(Collections.singletonMap(new TopicPartition(topicName,0),records)),
                Collections.singletonMap(topicName,Collections.emptyMap())
                );

        assertTrue(emptyDocs.isEmpty());

        indexerDB.addIndexer(openApiIndexer);
        doReturn(Collections.singletonList(openApiIndexer)).when(service).getAllDocs(any(),any());
        var router = indexerDB.getRouter();

        var oneDoc = RecordConsumer.processBatch(
            new ConsumerRecords<>(Collections.singletonMap(new TopicPartition(topicName,0),records)),
            router
        );

        System.out.println(oneDoc);
        Assertions.assertFalse(oneDoc.isEmpty());

    }
 @Test
    void processBatchWithNoFieldValueTest() throws IOException {
        var mapper = new ObjectMapper();
        var service = Mockito.spy(new OpenSearchService(openSearchClient));
        IndexerDB indexerDB = new IndexerDB(service);
        var openApiIndexer = mapper.readValue(
            new File("./src/test/resources/json-files/indexer.json"),
            EricOssAssuranceIndexerIndexer.class
        );
        var topicName = openApiIndexer.getSource().getName();

        indexerDB.addIndexer(openApiIndexer);
        doReturn(Collections.singletonList(openApiIndexer)).when(service).getAllDocs(any(),any());
        var router = indexerDB.getRouter();

        List<ConsumerRecord<String, GenericRecord>> records = new ArrayList<>();

        var schema = new Schema.Parser().parse(new File("./src/test/resources/avro-schemas/twofields.avsc"));
        var record = new GenericRecordBuilder(schema)
                .set("c", "valueForC")
                .set("v", null)
                .build();

        records.add(new ConsumerRecord<String,GenericRecord >(topicName, 0, 0, null, record));
        var noDoc = RecordConsumer.processBatch(
            new ConsumerRecords<>(Collections.singletonMap(new TopicPartition(topicName,0),records)),
            router
        );
        assertTrue(noDoc.isEmpty());
    }

    @Test
    void consumerLoopTest(){

        var service = new OpenSearchService(openSearchClient);
        IndexerDB indexerDB = new IndexerDB(service){
            @Override
            public ImmutableMap<String, Map<String, List<Writer>>> getRouter() {
                return ImmutableMap.copyOf(Collections.emptyMap());
            }
        };
        try {
            RecordConsumer recordConsumer = new RecordConsumer(consumerProps, indexerDB, service);
            recordConsumer.start();
            recordConsumer.stopAndWait();
        } catch (Exception e) {
            Assertions.fail(e);
        }

    }
    @Test
    void consumerActionTest() throws IOException {
        var schema = new Schema.Parser().parse(new File("./src/test/resources/avro-schemas/schema2.avsc"));
        var arecord = new GenericRecordBuilder(schema)
                    .set("c1", "context 1...")
                    .set("c2", "context 2...")
                    .set("rv1", 100.1)
                    .set("rv2", 100.2)
                    .set("ri1", "info 1:")
                    .set("ri2", "info 2:")
                    .build();
        var consumerRecord = new ConsumerRecord<String, GenericRecord>("topic", 0, 0, null, arecord);

        try (var consumer = mock(KafkaConsumer.class)) {
            when(consumer.poll(any())).thenReturn(
                    new ConsumerRecords<>(Collections.singletonMap(new TopicPartition("topic", 0),
                    Collections.singletonList(consumerRecord))));
            var service = new OpenSearchService(openSearchClient);
            var indexerDB = new IndexerDB(service);
            // Need a RecordConsumer to check metrics recorded for the Counters
            RecordConsumer recordConsumer = new RecordConsumer(consumerProps, indexerDB, service);
            recordConsumer.start();

            ImmutableMap<String, Map<String, List<Writer>>> router = ImmutableMap.copyOf(Collections.emptyMap());
            recordConsumer.consumerAction(service, consumer, router);

            Counter micrometerRegistryCounter = Metrics.counter(IndexerMetrics.RECORDS_RECEIVED_COUNT.getMetricName(),
                AIS_METRIC_TAG, IndexerMetrics.RECORDS_RECEIVED_COUNT.getMetricName());
            Counter recConsumerRegCounter = RecordConsumer.registry.find(IndexerMetrics.RECORDS_RECEIVED_COUNT.getMetricName()).counter();
            assertEquals(micrometerRegistryCounter.getId().getName(), recConsumerRegCounter.getId().getName());
            System.out.println("**> Micrometer.Metrics::globalRegistry received_records: " + micrometerRegistryCounter.count());
            System.out.println("**> RecordConsumer::registry received_records: " + recConsumerRegCounter.count());

            micrometerRegistryCounter = Metrics.counter(IndexerMetrics.RECORDS_PROCESSED_COUNT.getMetricName(),
                AIS_METRIC_TAG, IndexerMetrics.RECORDS_PROCESSED_COUNT.getMetricName());
            recConsumerRegCounter = RecordConsumer.registry.find(IndexerMetrics.RECORDS_PROCESSED_COUNT.getMetricName()).counter();
            assertEquals(micrometerRegistryCounter.getId().getName(), recConsumerRegCounter.getId().getName());
            System.out.println("**> Micrometer.Metrics::globalRegistry processed_records: " + micrometerRegistryCounter.count());
            System.out.println("**> RecordConsumer::registry processed_records: " + recConsumerRegCounter.count());

        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    @Test
    void consumerIsRunningTest() {
        var service = new OpenSearchService(openSearchClient);
        IndexerDB indexerDB = new IndexerDB(service);
        try {
            RecordConsumer recordConsumer = new RecordConsumer(consumerProps, indexerDB, service);
            recordConsumer.start();
            // The thread will die, but we rely on the fact that the
            // thread will die after some retries, i.e., way after the next line is executed ...
            assertTrue(recordConsumer.isRunning());
            recordConsumer.stopAndWait();
            Assertions.assertFalse(recordConsumer.isRunning());
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    @Test
    void getRegisteredMetricCounterTest () {
        var service = new OpenSearchService(openSearchClient);
        IndexerDB indexerDB = new IndexerDB(service);
        try {
            RecordConsumer recordConsumer = new RecordConsumer(consumerProps, indexerDB, service);
            recordConsumer.start();

            Search searchCriteria = recordConsumer.getMeterRegistry().find(IndexerMetrics.RECORDS_RECEIVED_COUNT.getMetricName())
                .tag(AIS_METRIC_TAG, IndexerMetrics.RECORDS_RECEIVED_COUNT.getMetricName());
            Counter returnedCounter = recordConsumer.getRegisteredMetricCounter(searchCriteria);
            assertNotNull(returnedCounter);

            searchCriteria = recordConsumer.getMeterRegistry().find(IndexerMetrics.RECORDS_RECEIVED_COUNT.getMetricName())
                .tag("TAG_FOR_Failure", IndexerMetrics.RECORDS_RECEIVED_COUNT.getMetricName());
            returnedCounter = recordConsumer.getRegisteredMetricCounter(searchCriteria);
            assertNull(returnedCounter);

            searchCriteria = recordConsumer.getMeterRegistry().find(IndexerMetrics.RECORDS_RECEIVED_COUNT.getMetricName())
                .tag(AIS_METRIC_TAG, "BAD_TAG_VALUE");
            returnedCounter = recordConsumer.getRegisteredMetricCounter(searchCriteria);
            assertNull(returnedCounter);

            searchCriteria = recordConsumer.getMeterRegistry().find(IndexerMetrics.RECORDS_PROCESSED_COUNT.getMetricName())
                .tag(AIS_METRIC_TAG, IndexerMetrics.RECORDS_PROCESSED_COUNT.getMetricName());
            returnedCounter = recordConsumer.getRegisteredMetricCounter(searchCriteria);
            assertNotNull(returnedCounter);

        } catch (Exception e) {
            Assertions.fail(e);
        }

    }

}
