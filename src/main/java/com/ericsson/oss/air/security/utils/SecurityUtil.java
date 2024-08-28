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
package com.ericsson.oss.air.security.utils;

import com.ericsson.oss.air.security.utils.exceptions.InternalRuntimeException;
import lombok.experimental.UtilityClass;
import org.apache.coyote.http11.Http11NioProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility Class for Keystore operations
 */
@UtilityClass
public class SecurityUtil {
    private static final String DEFAULT_SSL_HOSTNAME_CONFIG_NAME = "_default_";
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityUtil.class);

    /**
     * Reloads the tomcat instance with the latest truststore and keystore instances by updating Http11NioProtocol
     */
    public static void reloadSSLHostConfig(Http11NioProtocol protocol) {
        LOGGER.info("Trying to reload SSLHostConfig");
        try {
            LOGGER.info("Reloading protocol with updated keystore and truststore...");
            protocol.reloadSslHostConfig(DEFAULT_SSL_HOSTNAME_CONFIG_NAME);
            LOGGER.info("Successfully reloaded the SSLHostConfig");
        } catch (Exception e) {

            LOGGER.error("Cannot reset the SSLHostConfigCertificate. Server has to be restarted for certificate changes :: {} ",
                    e.getMessage());
            throw new InternalRuntimeException(String.format("Failed to add certificates for alias: %s", "alias"), e);
        }
    }
}
