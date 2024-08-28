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

package com.ericsson.oss.air.security;

import com.ericsson.oss.air.security.config.CertificateId;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.util.Optional;

@Component
@ConditionalOnProperty(value = "security.tls.enabled", havingValue = "true")
public class RestTemplateReloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestTemplateReloader.class);

    @Value("${adp-certificate.discovery.keyPassword}")
    private String keyPassword;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SslBundles sslBundles;


    /**
     * This method will be used to reset ClientHttpRequestFactory in RestTemplate if keystore, truststore updated.
     */
    public void reload() {
        Optional<SslBundle> sslBundle = Optional.ofNullable(sslBundles.getBundle(CertificateId.SERVER.getAlias()));
        sslBundle.ifPresent(bundle -> restTemplate.setRequestFactory(getRequestFactory(bundle)));
        LOGGER.info("RestTemplate reloaded successfully with updated keystore, truststore");
    }

    /**
     * This method is responsible to return ClientHttpRequestFactory based on keystore, truststore
     * and returned ClientHttpRequestFactory will be used in RestTemplate for mutual tls with cts server.
     *
     * @param sslBundle sslbundle for ssl context
     * @return the request factory
     */
    private ClientHttpRequestFactory getRequestFactory(SslBundle sslBundle) {
        final SSLContext sslContext = sslBundle.createSslContext();
        final SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create().setSslContext(sslContext).build();
        final HttpClientConnectionManager manager = PoolingHttpClientConnectionManagerBuilder.create().setSSLSocketFactory(sslSocketFactory).build();
        final CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(manager).build();
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}