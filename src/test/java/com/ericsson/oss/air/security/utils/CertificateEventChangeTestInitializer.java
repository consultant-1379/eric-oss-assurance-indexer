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

import org.apache.commons.io.FileUtils;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;


public class CertificateEventChangeTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            FileUtils.deleteDirectory(new File("target/security"));
            File source = ResourceUtils.getFile("src/test/resources/security/");
            FileUtils.copyDirectoryToDirectory(source, new File("target/"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
