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

package com.ericsson.oss.air.security.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Rest Template Details.
 */

@Configuration
public class RestTemplateConfiguration {

    @Bean
    @ConditionalOnProperty(
            value = "security.tls.enabled",
            havingValue = "true")
    RestTemplate tlsRestTemplate(SslBundles sslBundles) {
        return new RestTemplateBuilder().setSslBundle(sslBundles.getBundle(CertificateId.SERVER.getAlias())).build();
    }
}