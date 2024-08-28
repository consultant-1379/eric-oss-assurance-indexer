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

package com.ericsson.oss.air.log;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class LoggerHandler {

    public static final String FACILITY_KEY = "facility";
    public static final String SUBJECT_KEY = "subject";
    public static final String AUDIT_LOG = "log audit";


    public void startSecurityLog() {
        MDC.put(FACILITY_KEY, AUDIT_LOG);
        MDC.put(SUBJECT_KEY, "ASSURANCE-INDEXER");
    }

    public void endSecurityLog() {
        MDC.remove(FACILITY_KEY);
        MDC.remove(SUBJECT_KEY);
    }
}
