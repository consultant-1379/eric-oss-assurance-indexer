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
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@TestPropertySource(properties = "logging.config=classpath:logback-https.xml")
public class LogHttpsAppenderTest extends LoggingWiremockTestSetup {
    @BeforeEach
    public void setup() {
        WireMockConfiguration configuration = options()
                .httpsPort(9443)
                .keystorePath(KEYSTORE_PATH)
                .keystorePassword("changeit")
                .keyManagerPassword("changeit")
                .keystoreType("pkcs12");
        WireMockServer wireMockServer = new WireMockServer(configuration);
        wireMockServer.start();
        configureFor("https", "localhost", 9443);
        stubFor(post(anyUrl()).withPort(9443).withScheme("https").withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .willReturn(ok()));
    }

    //@Test
    public void checkLoggingHttpsAppender() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        Appender<ILoggingEvent> httpAppender = lc.getLogger("root").getAppender("https");
        assertThat(httpAppender).isNotNull().matches(LifeCycle::isStarted);
        lc.stop();
    }
}
