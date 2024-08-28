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

import com.ericsson.adp.security.certm.certificatewatcher.TlsContext;
import com.ericsson.oss.air.log.LoggerHandler;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.function.Consumer;

import static com.ericsson.oss.air.security.utils.SecurityUtil.reloadSSLHostConfig;

public class CertificateEventChangeDetector implements ChangeDetector {
    private static final Logger LOG = LoggerFactory.getLogger(CertificateEventChangeDetector.class);
    private final CustomSslStoreBundle sslStoreBundle;
    @Getter
    private final Flux<TlsContext> tlsContextFlux;
    private Disposable disposable;
    @Autowired
    private LoggerHandler loggerHandler;

    public CertificateEventChangeDetector(Flux<TlsContext> tlsContextFlux, CustomSslStoreBundle sslStoreBundle) {
        this.tlsContextFlux = tlsContextFlux;
        this.sslStoreBundle = sslStoreBundle;
        this.loggerHandler = new LoggerHandler();
    }

    @PostConstruct
    public void init() {
        disposable = subscribe(tlsContextFlux, getTlsContextConsumer());
    }

    @Override
    public Disposable subscribe(Flux<TlsContext> tlsContextFlux, Consumer<TlsContext> tlsContextConsumer) {
        return tlsContextFlux
                .doOnEach(tlsContextSignal -> LOG.debug("Signal received before filter: {}", tlsContextSignal.get()))
                .onErrorContinue((throwable, context) ->
                        LOG.error("Failed to process TlsContext, skipping the signal {}, due to {}", context, throwable.getMessage()))
                .subscribe(tlsContextConsumer);
    }

    @Override
    @PreDestroy
    public void shutdown() {
        disposable.dispose();
    }


    private Consumer<TlsContext> getTlsContextConsumer() {
        return tlsData -> {
            boolean isUpdated = sslStoreBundle.setContext(tlsData);
            if (isUpdated) {
                loggerHandler.startSecurityLog();
                LOG.debug("SSL context updated with new keystore and/or truststore for:: {}", tlsData.getName());
                loggerHandler.endSecurityLog();
                reloadSSLHostConfig(sslStoreBundle.getProtocol());
            } else {
                LOG.debug("Skipping reload, context not changed:: {}", tlsData.getName());
            }
        };
    }
}



