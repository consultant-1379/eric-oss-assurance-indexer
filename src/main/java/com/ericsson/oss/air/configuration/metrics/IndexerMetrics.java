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

/**
 * Enum defining the custom AIS Metric Names for Data Dictionary item and run time data counts.
 * At runtime, all metric names will be prefixed with
 * the AIS service acronym 'ais'.  For example '{@code index_records_received_total}' will appear as
 * '{@code  ais_{HOST}_index_records_received_total}' in the {@code /actuator/prometheus} output.
 *
 * Naming conventions:
 * Generally comply with the design rules and guidelines at
 * <a href="https://eteamspace.internal.ericsson.com/display/AA/PM+design+rules#PMdesignrules-Relevantmetrics)">PM Design Rules</a>
 * <ul>
 * <li>All metrics have the units appended to the name</li>
 * <li>For metrics representing a cumulative count, e.g. total number of PM definitions in the data
 * dictionary, word 'total' is appended rather than the units</li>
 * <li>Maximum length of a custom metric name is 50 characters</li>
 * <li>To handle restarts and multiple pod instances of the application, the pod-name is appended
 * as a label</li>
 * </ul>
 *
 * Note: the enum is extensible because it implements an Interface.
 */

public enum IndexerMetrics implements MetricDescription {

    RECORDS_RECEIVED_COUNT {
        @Override
        public String getMetricName() {
            return("ais_index_records_received_total");
        }
    },
    RECORDS_PROCESSED_COUNT {
        @Override
        public String getMetricName() {
            return("ais_index_records_processed_total");
        }
    },
    APP_STARTUP_TIME {
        @Override
        public String getMetricName() {
            return("ais_startup_time_seconds");
        }
    };
    public static final String AIS_METRIC_TAG = "ais_custom_metric";

}
