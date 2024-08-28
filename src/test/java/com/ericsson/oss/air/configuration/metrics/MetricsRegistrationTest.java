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

package com.ericsson.oss.air.configuration.metrics;


import com.ericsson.oss.air.util.IndexerProcessingTimeTracker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Since there could be multiple pods we need a separate metric for each one.  Use the pod_name
 * as a tag.  This is a dynamic tag.  Do not create the metric once at init time but every
 * time the metric is updated.  If the name and tag are the same, no new metric will be created.
 */

class MetricsRegisterTest {
    private final static MeterRegistry registry = new SimpleMeterRegistry();

    private IndexerMetricsRegistration metricsRegister;

    @BeforeEach
    public void setUp() {
        metricsRegister = new IndexerMetricsRegistration(registry);
    }

    @AfterEach
    public void tearDown() {
        registry.clear();
    }

    @Test
    void registerMeterRecordsReceivedCountTest() {
        Counter myCounter = IndexerMetricsRegistration.registerRecordsReceivedCount(registry);
        Assertions.assertNotNull(myCounter);
        var oldCounterValue = myCounter.count();
        Meter.Id id1 = myCounter.getId();

        myCounter.increment();
        Assertions.assertEquals(1.0, myCounter.count() - oldCounterValue, 0.00001);

        // Register again and see if we can increment the same counter as above.
        Counter myCounter2 = IndexerMetricsRegistration.registerRecordsReceivedCount(registry);
        Assertions.assertNotNull(myCounter2);
        Meter.Id id2 = myCounter.getId();
        assertEquals(id1, id2);

        oldCounterValue = myCounter2.count();
        myCounter2.increment(1);
        Assertions.assertEquals(1.0, myCounter2.count() - oldCounterValue, 0.00001);

        Counter myCounter3 = IndexerMetricsRegistration.registerRecordsReceivedCount(registry);
        Assertions.assertNotNull(myCounter3);
        Meter.Id id3 = myCounter.getId();
        assertEquals(id1, id3);

        oldCounterValue = myCounter3.count();
        myCounter3.increment(25);
        Assertions.assertEquals(25.0, myCounter3.count() - oldCounterValue, 0.00001);

    }

    @Test
    void registerMeterRecordsProcessedCountTest() {
        final Counter myCounter = IndexerMetricsRegistration.registerRecordsProcessedCount(registry);
        Assertions.assertNotNull(myCounter);
        var oldCounterValue = myCounter.count();
        Meter.Id id1 = myCounter.getId();

        // Test increment of the counter
        myCounter.increment(1.0);
        Assertions.assertEquals(1.0, myCounter.count() - oldCounterValue, 0.00001);

        // Register again and see if we can increment the same counter as above.
        Counter myCounter2 = IndexerMetricsRegistration.registerRecordsProcessedCount(registry);
        Assertions.assertNotNull(myCounter2);
        Meter.Id id2 = myCounter.getId();
        assertEquals(id1, id2);

        oldCounterValue = myCounter2.count();
        myCounter2.increment(1.0);
        Assertions.assertEquals(1.0, myCounter2.count() - oldCounterValue, 0.00001);

        Counter myCounter3 = IndexerMetricsRegistration.registerRecordsProcessedCount(registry);
        Assertions.assertNotNull(myCounter3);
        Meter.Id id3 = myCounter.getId();
        assertEquals(id1, id3);

        oldCounterValue = myCounter3.count();
        myCounter3.increment(25.0);
        Assertions.assertEquals(25.0, myCounter3.count() - oldCounterValue, 0.00001);
    }

    @Test
    void registerIndexerProcessingTimeTest() {
        final IndexerProcessingTimeTracker indexerProcessingTimeTracker = new IndexerProcessingTimeTracker();
        metricsRegister.registerIndexerProcessingTime(indexerProcessingTimeTracker);
        Gauge indexerProcessingTime = registry.find(IndexerMetrics.APP_STARTUP_TIME.getMetricName()).gauge();

        Assertions.assertNotNull(indexerProcessingTime);
        Assertions.assertEquals(0.0, indexerProcessingTime.value(), 0.00001);
        Meter.Id id1 = indexerProcessingTime.getId();

        // Test for TimeTracker call-back and calculation of the metric value.
        indexerProcessingTimeTracker.setAisProcessingStartTime(100L);
        indexerProcessingTimeTracker.setAisProcessingFinishTime(1111L);
        indexerProcessingTime = registry.find(IndexerMetrics.APP_STARTUP_TIME.getMetricName()).gauge();

        // Test for use of the same metric.
        Meter.Id id2 = indexerProcessingTime.getId();
        Assertions.assertEquals(1.011, indexerProcessingTime.value(), 0.00001);
        assertEquals(id1, id2);

        // Try another one and make sure we get the same meter.  (To get dynamic tags we
        // have to register every time we want to update the metric.)
        metricsRegister.registerIndexerProcessingTime(indexerProcessingTimeTracker);
        Gauge indexerProcessingTime2 = registry.find(IndexerMetrics.APP_STARTUP_TIME.getMetricName()).gauge();
        Assertions.assertNotNull(indexerProcessingTime2);
        Meter.Id id3 = indexerProcessingTime2.getId();
        assertEquals(id3, id1);
        indexerProcessingTimeTracker.setAisProcessingStartTime(5000L);
        indexerProcessingTimeTracker.setAisProcessingFinishTime(10000L);
        indexerProcessingTime2 = registry.find(IndexerMetrics.APP_STARTUP_TIME.getMetricName()).gauge();
        Assertions.assertEquals(5.0, indexerProcessingTime2.value(), 0.00001);

        // Test error path in the TimeTracker
        Gauge indexerProcessingTime3 = registry.find(IndexerMetrics.APP_STARTUP_TIME.getMetricName()).gauge();
        Assertions.assertNotNull(indexerProcessingTime3.value());
        Meter.Id id4 = indexerProcessingTime3.getId();
        assertEquals(id4, id1);
        indexerProcessingTimeTracker.setAisProcessingStartTime(5000L);
        indexerProcessingTimeTracker.setAisProcessingFinishTime(1000L);
        indexerProcessingTime3 = registry.find(IndexerMetrics.APP_STARTUP_TIME.getMetricName()).gauge();
        Assertions.assertEquals(0.0, indexerProcessingTime3.value(), 0.00001);
    }

    @Test
    void getAppStartUpTimeMetricTest () {
        final IndexerProcessingTimeTracker indexerProcessingTimeTracker = new IndexerProcessingTimeTracker();
        metricsRegister.registerIndexerProcessingTime(indexerProcessingTimeTracker);
        indexerProcessingTimeTracker.setAisStartTime(5000L);
        indexerProcessingTimeTracker.setAisProcessingFinishTime(10000L);
        Assertions.assertNotNull(metricsRegister.getAppStartUpTimeMetric());
        tearDown();
        Assertions.assertNull(metricsRegister.getAppStartUpTimeMetric());
    }

    @Test
    void getAppAStartUpTimeMetricValueTest () {
        final IndexerProcessingTimeTracker indexerProcessingTimeTracker = new IndexerProcessingTimeTracker();
        metricsRegister.registerIndexerProcessingTime(indexerProcessingTimeTracker);
        Assertions.assertEquals(0.0, metricsRegister.getAppStartUpTimeValue(), 0.00001);
        indexerProcessingTimeTracker.setAisProcessingStartTime(5000L);
        indexerProcessingTimeTracker.setAisProcessingFinishTime(10000L);
        Assertions.assertEquals(5.0, metricsRegister.getAppStartUpTimeValue(), 0.00001);

    }

    @SneakyThrows
    @Test
    void getHostNameTest() {
        String testHostName = IndexerMetricsRegistration.getHostName();
        System.out.println("metricsRegister's returned hostname: " + testHostName);
        Assertions.assertNotNull(IndexerMetricsRegistration.getHostName());
        assertEquals(InetAddress.getLocalHost().getHostName(), IndexerMetricsRegistration.getHostName());
    }

    @Test
    void getPodNameTest () {
        assertEquals(IndexerMetricsRegistration.DEFAULT_POD_NAME,
            IndexerMetricsRegistration.getPodName("junk"));
        assertEquals(IndexerMetricsRegistration.DEFAULT_POD_NAME,
            IndexerMetricsRegistration.getPodName("a_very_long_pod_name_that_is_junk_too"));
        assertEquals(IndexerMetricsRegistration.DEFAULT_POD_NAME.concat("-657d95fddc-5qsbh"),
            IndexerMetricsRegistration.getPodName("eric-oss-assurance-indexer-657d95fddc-5qsbh"));
    }

}