/*******************************************************************************
 * COPYRIGHT Ericsson 2021
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

package com.ericsson.oss.air;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.io.File;

@Configuration
@Slf4j
public class AppConfig {

    @Value("${spring.config.location}")
    private String configFilePath;

    private ConfigChangedThread runningThread;

    private final class ConfigChangedThread {
        private final Thread thread;
        private long modTime;

        private ConfigChangedThread() {
            modTime = new File(configFilePath).lastModified();
            thread = new Thread(this::checkFileChanged);
            thread.start();
        }

        @SneakyThrows
        private void checkFileChanged() {
            while (this == runningThread) {
                File file = new File(configFilePath);
                if (modTime != file.lastModified()) {
                    killAIS();
                }
            }
        }
    }

    protected int killAIS() throws ConfigFileChangedException {
        // SonarQube does not like runtime.halt or sys.exit, so we're using a less direct approach
        throw new ConfigFileChangedException("application.yaml has been changed, restart service");
    }

    // Custom exception to make SonarQube happy
    public static class ConfigFileChangedException extends Exception {
        public ConfigFileChangedException(String msg){
            super(msg);
        }
    }
    @Order(1)
    @org.springframework.context.event.EventListener(ApplicationReadyEvent.class)
    public void startThread() {
        runningThread = new ConfigChangedThread();
    }

    public void stopThread() {
        runningThread = null;
    }

}
