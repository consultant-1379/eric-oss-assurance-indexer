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


import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "security.tls.enabled=true")
public abstract class AbstractTlsTestSetup extends AbstractTestSetup {
    // Placeholder for activating test profile with TLS enabled.
}
