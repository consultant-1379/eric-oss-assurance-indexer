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


import com.ericsson.adp.security.certm.certificatewatcher.CertificateWatcherProperties;
import com.ericsson.adp.security.certm.certificatewatcher.CertificateWatcherService;
import com.ericsson.adp.security.certm.certificatewatcher.TlsContext;
import com.ericsson.oss.air.security.config.SecurityProperties;
import com.ericsson.oss.air.security.utils.exceptions.InternalRuntimeException;
import org.apache.coyote.http11.Http11NioProtocol;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.file.Paths;

import static com.ericsson.oss.air.security.utils.SecurityTestUtils.SERVER_ALIAS;
import static com.ericsson.oss.air.security.utils.SecurityTestUtils.createTlsContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {CertificateWatcherService.class, CustomSslStoreBundle.class, DefaultSslBundleRegistry.class})
@EnableConfigurationProperties({CertificateWatcherProperties.class, SecurityProperties.class, ServerProperties.class})
class CertificateEventChangeTest extends AbstractTlsTestSetup {
    private static final String SERVER_PATH = "src/test/resources/security/server";
    private static final String SERVER_UPDATED_PATH = "src/test/resources/security/server-updated";

    @SpyBean
    private SecurityProperties securityProperties;

    @SpyBean
    private CertificateWatcherProperties certificateWatcherProperties;

    @SpyBean
    private CustomSslStoreBundle sslStoreBundle;

    @Test
    void verifyCertificateChangeEvents() {
        TlsContext tlsContext = createTlsContext(SERVER_PATH, certificateWatcherProperties, securityProperties);
        TlsContext tlsContextUpdate = createTlsContext(SERVER_UPDATED_PATH, certificateWatcherProperties, securityProperties);
        Flux<TlsContext> merged = Flux.merge(Flux.just(), Flux.just(tlsContext, tlsContextUpdate));

        // Set context for stores
        Http11NioProtocol protocol = mock(Http11NioProtocol.class);
        sslStoreBundle.setProtocol(protocol);
        CertificateEventChangeDetector certificateEventChangeDetector = new CertificateEventChangeDetector(merged, sslStoreBundle);
        certificateEventChangeDetector.init();
        Flux<TlsContext> tlsContextFluxFromDetector = certificateEventChangeDetector.getTlsContextFlux();

        StepVerifier.create(tlsContextFluxFromDetector.take(2))
                .expectSubscription()
                .assertNext(context -> {
                    assertThat(context.getName()).isEqualTo(SERVER_ALIAS);
                    assertThat(context.getKeyStore()).isPresent();
                    assertThat(context.getKeyStore().get().getPath()).endsWith(Paths.get("server/keystore.p12"));
                    assertThat(context.getTrustStore()).isPresent();
                    assertThat(context.getTrustStore().get().getPath()).endsWith(Paths.get("server/truststore.p12"));
                })
                .assertNext(context -> {
                    assertThat(context.getName()).isEqualTo(SERVER_ALIAS);
                    assertThat(context.getKeyStore()).isPresent();
                    assertThat(context.getKeyStore().get().getPath()).endsWith(Paths.get("server-updated/keystore.p12"));
                    assertThat(context.getTrustStore()).isPresent();
                    assertThat(context.getTrustStore().get().getPath()).endsWith(Paths.get("server-updated/truststore.p12"));
                })
                .verifyComplete();

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(protocol, times(2)).reloadSslHostConfig(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isEqualTo("_default_");
    }

    @Test
    void skipFluxSignalWhenFailureSetContextFails() {
        TlsContext tlsContext = TlsContext.builder().name("Test").build();
        Http11NioProtocol protocol = mock(Http11NioProtocol.class);
        CustomSslStoreBundle sslContext = mock(CustomSslStoreBundle.class);
        when(sslContext.setContext(any(TlsContext.class))).thenThrow(InternalRuntimeException.class);
        Flux<TlsContext> tlsContextFlux = Flux.just(tlsContext);
        CertificateEventChangeDetector eventChangeDetector = new CertificateEventChangeDetector(tlsContextFlux, sslContext);
        eventChangeDetector.init();
        Flux<TlsContext> tlsContextFluxFromDetector = eventChangeDetector.getTlsContextFlux();

        StepVerifier.create(tlsContextFluxFromDetector)
                .expectNextCount(1)
                .expectComplete()
                .verify();
        verify(protocol, times(0)).reloadSslHostConfig(anyString());
    }

    @Test
    void shouldContinueWhenReloadSslHostConfigFails() {
        CustomSslStoreBundle sslContext = mock(CustomSslStoreBundle.class);
        Http11NioProtocol protocol = spy(Http11NioProtocol.class);
        sslContext.setProtocol(protocol);

        Flux<TlsContext> just = Flux.just(TlsContext.builder().name("server").build());
        when(sslContext.setContext(any(TlsContext.class))).thenReturn(true);

        CertificateEventChangeDetector certificateEventChangeDetector = new CertificateEventChangeDetector(just, sslContext);
        certificateEventChangeDetector.init();
        Flux<TlsContext> tlsContextFluxFromDetector = certificateEventChangeDetector.getTlsContextFlux();
        doThrow(InternalRuntimeException.class).when(protocol).reloadSslHostConfig(anyString());

        StepVerifier.create(tlsContextFluxFromDetector)
                .expectNextCount(1)
                .verifyComplete();
        verify(protocol, times(0)).reloadSslHostConfig(anyString());
    }

    @Test
    void shouldNotReloadTomcatWhenClientSignals() {
        CustomSslStoreBundle sslContext = mock(CustomSslStoreBundle.class);
        Http11NioProtocol protocol = spy(Http11NioProtocol.class);
        sslContext.setProtocol(protocol);

        Flux<TlsContext> just = Flux.just(TlsContext.builder().name("client").build());
        when(sslContext.setContext(any(TlsContext.class))).thenReturn(true);

        CertificateEventChangeDetector certificateEventChangeDetector =
                new CertificateEventChangeDetector(just, sslContext);
        certificateEventChangeDetector.init();
        Flux<TlsContext> tlsContextFluxFromDetector = certificateEventChangeDetector.getTlsContextFlux();

        StepVerifier.create(tlsContextFluxFromDetector)
                .expectNextCount(1)
                .verifyComplete();
        verify(protocol, times(0)).reloadSslHostConfig(anyString());
    }
}