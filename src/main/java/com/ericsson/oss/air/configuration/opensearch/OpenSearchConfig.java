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

import com.ericsson.oss.air.security.CustomSslStoreBundle;
import com.ericsson.oss.air.security.config.CertificateId;
import com.ericsson.oss.air.security.utils.exceptions.InternalRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

@Configuration
@Slf4j
public class OpenSearchConfig {
    @Bean
    @ConditionalOnProperty(value = "security.tls.enabled", havingValue = "false")
    public OpenSearchClient createOpenSearchClient(OpenSearchProperties openSearchProperties) {
        return createOpenSearchClient(openSearchProperties.getClient().getHost(), openSearchProperties.getClient().getPort(), openSearchProperties.getClient().getScheme());
    }

    private static OpenSearchClient createOpenSearchClient(String host, Integer port, String scheme) {
        var httpHost = new org.apache.http.HttpHost(host, port, scheme);
        var restClient = RestClient.builder(httpHost).build();
        var transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new OpenSearchClient(transport);
    }

    @Bean
    @ConditionalOnProperty(value = "security.tls.enabled", havingValue = "true")
    @DependsOn("certificateSubscription")
    public OpenSearchClient createOpenSearchClientSecure(OpenSearchProperties openSearchProperties, SslBundles sslBundles) {
        try {
            return createOpenSearchClientSecure(openSearchProperties.getClient().getHost(), openSearchProperties.getClient().getPort(), openSearchProperties.getClient().getScheme(), sslBundles);
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | NoSuchSslBundleException e) {
            throw new InternalRuntimeException("Failed to create OpenSearchClient:: ", e);
        }
    }

    private static OpenSearchClient createOpenSearchClientSecure(String host, Integer port, String scheme, SslBundles sslBundles) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {
        final HttpHost httpHost = new HttpHost(scheme, host, port);
        CustomSslStoreBundle storeBundle = (CustomSslStoreBundle) sslBundles.getBundle(CertificateId.SERVER.getAlias()).getStores();

        final SSLContext sslcontext = SSLContextBuilder
                .create()
                .loadTrustMaterial(storeBundle.getTrustStore(), null)
                .loadKeyMaterial(storeBundle.getKeyStore(), storeBundle.getKeyStorePassword().toCharArray())
                .build();

        final ApacheHttpClient5TransportBuilder builder = ApacheHttpClient5TransportBuilder.builder(httpHost);
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            final TlsStrategy tlsStrategy;
            tlsStrategy = ClientTlsStrategyBuilder.create()
                    .setSslContext(sslcontext)
                    .build();

            final PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder
                    .create()
                    .setTlsStrategy(tlsStrategy)
                    .build();

            return httpClientBuilder.setConnectionManager(connectionManager);
        });
        return new OpenSearchClient(builder.build());
    }
}
