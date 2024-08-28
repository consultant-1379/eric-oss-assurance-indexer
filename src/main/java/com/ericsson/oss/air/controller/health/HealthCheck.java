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

package com.ericsson.oss.air.controller.health;

import com.ericsson.oss.air.RecordConsumer;
import com.ericsson.oss.air.log.LoggerHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health Check component for microservice chassis.
 * Any internal logic can change health state of the chassis.
 */

@Component("livenessCheck")
@Slf4j
public final class HealthCheck implements HealthIndicator {

    @Autowired
    RecordConsumer recordConsumer;

    @Autowired
    private LoggerHandler loggerHandler;

    @Value("${health.grace-count}")
    private int graceCount;

    public void setGraceCount(int graceCount) {
        log.info("TO BE USED IN TESTS ONLY! Resetting graceCount to {}", graceCount);
        synchronized (this) {
            this.graceCount = graceCount;
        }
    }

    @Override
    public Health health() {
        log.debug("Invoking chassis specific health check");
        if (graceCount > 0) {
            log.info("HealthCheck still in graceCount: {} ...", graceCount);
            synchronized (this) {
                // We do not care if the graceCount is negative, i.e.,
                // we can have a counter going down to -1, -2, -3, ...
                graceCount--;
            }
            return Health.up().build();
        }
        if (! recordConsumer.isRunning()) {
            loggerHandler.startSecurityLog();
            log.info("RecordConsumer thread is not running.");
            loggerHandler.endSecurityLog();
            return Health.down().withDetail("Error: ", "Record consumer thread is not running.").build();
        }
        return Health.up().build();
    }

}
