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
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

public interface ChangeDetector {

    Disposable subscribe(Flux<TlsContext> tlsContextFlux, Consumer<TlsContext> tlsContextConsumer);

    void shutdown();
}
