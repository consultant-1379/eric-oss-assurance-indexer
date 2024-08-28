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

package com.ericsson.oss.air.configuration.opensearch;

import com.ericsson.oss.air.IndexerDB;
import com.ericsson.oss.air.RecordConsumer;
import com.ericsson.oss.air.security.AbstractTestSetup;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {OpenSearchConfig.class, OpenSearchProperties.class})
@EnableConfigurationProperties({OpenSearchProperties.class})
public class OpenSearchConfigTlsTest extends AbstractTestSetup {
    @MockBean
    private IndexerDB indexerDB;

    @MockBean
    private RecordConsumer recordConsumer;

    @Autowired
    OpenSearchConfig openSearchConfig;

    @Autowired
    private OpenSearchProperties openSearchProperties;

    @Test
    public void createOpenSearchClient() {
        OpenSearchClient openSearchClient = openSearchConfig.createOpenSearchClient(openSearchProperties);
        assertThat(openSearchClient).isNotNull();
    }
}
