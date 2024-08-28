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

import lombok.Getter;

@Getter
public enum CertificateId {
    SERVER("server"),
    ROOT("root"),
    LOG("log"),
    PM_CA("pmca"),
    SEARCH_ENGINE("search_engine"),
    KAFKA("kafka");

    private final String alias;

    CertificateId(String alias) {
        this.alias = alias;
    }
}
