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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.server.Ssl;
import org.springframework.context.annotation.Configuration;

/**
 * Security configuration for SSL setup
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
    private static final String KEYSTORE_TYPE_PKCS_12 = "pkcs12";

    private Tls tls = new Tls();

    @Data
    public static class Tls {
        private Boolean enabled;
        private Integer port;
        private Ssl.ClientAuth clientAuth = Ssl.ClientAuth.NEED; // enable mTLS
        private String keystoreType = KEYSTORE_TYPE_PKCS_12;
    }
}
