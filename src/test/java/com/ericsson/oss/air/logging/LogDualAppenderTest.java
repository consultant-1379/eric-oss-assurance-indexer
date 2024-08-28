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

package com.ericsson.oss.air.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.LifeCycle;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@TestPropertySource(properties = "logging.config=classpath:logback-dual.xml")
public class LogDualAppenderTest extends LoggingWiremockTestSetup {

    @BeforeEach
    public void setup() {
        WireMockServer wireMockServer = new WireMockServer(9080);
        wireMockServer.start();
        configureFor(9080);
        stubFor(post(anyUrl()).withHeader("Content-Type", equalTo("application/json; charset=UTF-8")).willReturn(ok()));
    }

    //@Test
    public void checkLoggingDualAppender() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        Appender<ILoggingEvent> httpAppender = lc.getLogger("root").getAppender("http");
        Appender<ILoggingEvent> jsonAppender = lc.getLogger("root").getAppender("json");
        assertThat(httpAppender).isNotNull().matches(LifeCycle::isStarted);
        assertThat(jsonAppender).isNotNull().matches(LifeCycle::isStarted);
        lc.stop();
    }
}
