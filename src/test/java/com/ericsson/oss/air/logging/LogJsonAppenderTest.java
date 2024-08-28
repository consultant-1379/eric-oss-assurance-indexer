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
import com.ericsson.oss.air.CoreApplication;
import com.ericsson.oss.air.IndexerDB;
import com.ericsson.oss.air.RecordConsumer;
import com.ericsson.oss.air.security.AbstractTestSetup;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {CoreApplication.class})
@TestPropertySource(properties = "logging.config=classpath:logback-json.xml")
public class LogJsonAppenderTest extends AbstractTestSetup {
    @MockBean
    private IndexerDB indexerDB;

    @MockBean
    private RecordConsumer recordConsumer;

    //@Test
    public void checkLoggingJsonAppender() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        Appender<ILoggingEvent> jsonAppender = lc.getLogger("root").getAppender("json");
        AssertionsForClassTypes.assertThat(jsonAppender).isNotNull().matches(LifeCycle::isStarted);
        lc.stop();
    }
}
