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

import com.ericsson.adp.security.certm.certificatewatcher.CertificateWatcherService;
import com.ericsson.adp.security.certm.certificatewatcher.TlsContext;
import com.ericsson.oss.air.security.CertificateEventChangeDetector;
import com.ericsson.oss.air.security.ChangeDetector;
import com.ericsson.oss.air.security.CustomSslStoreBundle;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Objects;

@Configuration
@ConditionalOnProperty(value = "security.tls.enabled", havingValue = "true")
public class CertificateChangeConfiguration {
    @Bean
    public Flux<TlsContext> certificateSubscription(CertificateWatcherService certificateWatcherService, CustomSslStoreBundle sslStoreBundle) {
//      certificateId will be used as alias and should be the same as the stores relative directory
//      READ_PATH/certificateId/keystore.

        ArrayList<Flux<TlsContext>> fluxes = new ArrayList<>();
        for (CertificateId certificateId : CertificateId.values()) {
            // Turn to hot stream, publish and connect to upstream without closing or resetting the published stream
            Flux<TlsContext> tlsContextFlux = certificateWatcherService.observe(certificateId.getAlias()).publish().autoConnect();
            // Await until stores generated and emit the initial signal
            TlsContext tlsContext = Objects.requireNonNull(tlsContextFlux.blockFirst());
            sslStoreBundle.setContext(Objects.requireNonNull(tlsContext));
            fluxes.add(tlsContextFlux);
        }
        return Flux.merge(fluxes);
    }

    @Bean
    public ChangeDetector changeDetector(Flux<TlsContext> tlsContextFlux, CustomSslStoreBundle bundle) {
        return new CertificateEventChangeDetector(tlsContextFlux, bundle);
    }
}