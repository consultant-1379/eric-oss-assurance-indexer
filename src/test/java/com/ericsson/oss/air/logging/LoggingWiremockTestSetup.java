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

import com.ericsson.oss.air.CoreApplication;
import com.ericsson.oss.air.IndexerDB;
import com.ericsson.oss.air.RecordConsumer;
import com.ericsson.oss.air.security.AbstractTestSetup;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {CoreApplication.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class LoggingWiremockTestSetup extends AbstractTestSetup {
    public static final String KEYSTORE_PATH = "src/test/resources/security/log-appender/keystore.jks";

    static {
        System.setProperty("LOGSTASH_DESTINATION", "localhost");
        System.setProperty("LOGSTASH_RECONNECTION_DELAY", "1");
        System.setProperty("ERIC_LOG_TRANSFORMER_KEYSTORE", KEYSTORE_PATH);
        System.setProperty("ERIC_LOG_TRANSFORMER_KEYSTORE_PW", "changeit");
        System.setProperty("ERIC_LOG_TRANSFORMER_TRUSTSTORE", KEYSTORE_PATH);
        System.setProperty("ERIC_LOG_TRANSFORMER_TRUSTSTORE_PW", "changeit");
    }

    @MockBean
    private IndexerDB indexerDB;

    @MockBean
    private RecordConsumer recordConsumer;

    @AfterAll
    public static void stop() {
        WireMock.shutdownServer();
    }
}
