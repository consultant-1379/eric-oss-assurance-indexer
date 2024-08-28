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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Open Search client configuration
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "open-search")
public class OpenSearchProperties {

    private Options client = new Options();
    @JsonProperty("client-tls")
    private Options clientTls = new Options();

    @Data
    public static class Options {
        private String host;
        private Integer port;
        private String scheme;
    }
}
