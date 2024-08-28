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

import com.ericsson.adp.security.certm.certificatewatcher.CertificateWatcherProperties;
import com.ericsson.oss.air.CoreApplication;
import com.ericsson.oss.air.IndexerDB;
import com.ericsson.oss.air.RecordConsumer;
import com.ericsson.oss.air.security.AbstractTlsTestSetup;
import com.ericsson.oss.air.security.config.SecurityProperties;
import com.ericsson.oss.air.security.utils.exceptions.InternalRuntimeException;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {CoreApplication.class})
@EnableConfigurationProperties({CertificateWatcherProperties.class, SecurityProperties.class, ServerProperties.class, OpenSearchProperties.class})
public class OpenSearchConfigTest extends AbstractTlsTestSetup {
    @MockBean
    private IndexerDB indexerDB;

    @MockBean
    private RecordConsumer recordConsumer;

    @Autowired
    private OpenSearchProperties openSearchProperties;

    @Autowired
    private OpenSearchConfig openSearchConfig;


    @Test
    public void createTlsEnabledOpenSearchClient() {
        OpenSearchClient openSearchClient = openSearchConfig.createOpenSearchClientSecure(openSearchProperties, mock(SslBundles.class));
        assertThat(openSearchClient).isNotNull();
    }

    @Test
    public void createTlsEnabledOpenSearchClientFails() {
        SslBundles sslBundleRegistry = mock(SslBundles.class);
        when(sslBundleRegistry.getBundle("server")).thenThrow(NoSuchSslBundleException.class);
        assertThatThrownBy(() -> new OpenSearchConfig().createOpenSearchClientSecure(openSearchProperties, sslBundleRegistry))
                .isInstanceOf(InternalRuntimeException.class)
                .hasMessageContaining("Failed to create OpenSearchClient");
    }
}
