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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;

import static com.ericsson.oss.air.configuration.metrics.IndexerMetrics.AIS_METRIC_TAG;

@Slf4j
@Configuration
public class IndexerMetricsRegistration {
    private final MeterRegistry registry;
    static final String DEFAULT_POD_NAME = "indexer";
    @Autowired
    public IndexerMetricsRegistration(final MeterRegistry registry)
    {
     // Empty constructor for SonarLint error in ApiImplTest failure for no visible constructors
     // in class IndexerMetricsRegistration.  Could also throw UnsupportedOperationException.
        this.registry = registry;
        Metrics.addRegistry(registry);
    }

    @SuppressWarnings("java:S1166")
    @SneakyThrows
    public static String getHostName() {
        return InetAddress.getLocalHost().getHostName();
    }
    /**
     * Get the pod name if running on kubernetes, or set a default one for testing if the
     * hostname (pod name) is not available.  The pod name can be used as a dynamic tag for
     * handling multiple instances of a service (scaling up or down) and if a single pod
     * restarts and we wish to show continuation of the service in time-series graphs of
     * the metrics.
     * @returns String
     * @parameters null
    */
    @SuppressWarnings("java:S1166")
    @SneakyThrows
    public static String getPodName(String hostname) {
        final String kubernetesPodNamePrefix = "eric-oss-assurance-indexer";
        final var podNamePrefixLength = kubernetesPodNamePrefix.length();

        var hostnameLength = hostname.length();
        String returnsPodName;
        // If hostname doesn't start with 'eric-oss-assurance-indexer', we are not on
        // kubernetes, return "indexer". Otherwise, return the substring after this app-name.
        if (hostnameLength < kubernetesPodNamePrefix.length()) {
            returnsPodName = DEFAULT_POD_NAME;
        } else if (hostname.startsWith(kubernetesPodNamePrefix)) {
            // Return the substring after "eric-oss-assurance-indexer" as a unique tag.
            returnsPodName = DEFAULT_POD_NAME.concat(hostname.substring(podNamePrefixLength));
        } else { returnsPodName = DEFAULT_POD_NAME; }

        return returnsPodName;
    }

    public static Counter registerRecordsReceivedCount(MeterRegistry registryParm) {
         return Metrics.counter(IndexerMetrics.RECORDS_RECEIVED_COUNT.getMetricName(), AIS_METRIC_TAG,
            IndexerMetrics.RECORDS_RECEIVED_COUNT.getMetricName());
    }

    public static Counter registerRecordsProcessedCount(MeterRegistry registryParm) {
        return Metrics.counter(IndexerMetrics.RECORDS_PROCESSED_COUNT.getMetricName(), AIS_METRIC_TAG,
            IndexerMetrics.RECORDS_PROCESSED_COUNT.getMetricName());
    }
    /**
     * Register the metric to record AIS' startup time for all target services
     * @param indexerProcessingTimeTracker
     *          The IndexerProcessingTimeTracker object instance for used to calculate the
     *          value to report.
     */
    @Autowired
    public void registerIndexerProcessingTime(final IndexerProcessingTimeTracker indexerProcessingTimeTracker) {
        Gauge.builder(IndexerMetrics.APP_STARTUP_TIME.getMetricName(),
                indexerProcessingTimeTracker, IndexerProcessingTimeTracker::getAisProcessingTime)
            .tags(AIS_METRIC_TAG, IndexerMetrics.APP_STARTUP_TIME.getMetricName())
            .register(registry);
    }
    public Gauge getAppStartUpTimeMetric() {
        return registry.find(IndexerMetrics.APP_STARTUP_TIME.getMetricName()).gauge();
    }
    public double getAppStartUpTimeValue() {
        Gauge indexerProcessingTime = getAppStartUpTimeMetric();
        if (indexerProcessingTime == null) {
            log.error("indexerProcessingTime is null! Cannot get StartUpTime Value. Returning 0.0.");
            return 0.0;
        } else {
            return indexerProcessingTime.value();
        }
    }

}