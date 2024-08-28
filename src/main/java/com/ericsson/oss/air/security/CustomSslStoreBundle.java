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

import com.ericsson.adp.security.certm.certificatewatcher.KeyStoreItem;
import com.ericsson.adp.security.certm.certificatewatcher.TlsContext;
import com.ericsson.adp.security.certm.certificatewatcher.TrustStoreItem;
import com.ericsson.oss.air.security.config.CertificateId;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.apache.coyote.http11.Http11NioProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.stereotype.Component;

import java.security.KeyStore;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.ericsson.oss.air.security.utils.KeystoreUtil.*;

@Getter
@Component
public class CustomSslStoreBundle implements SslStoreBundle {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomSslStoreBundle.class);

    @Setter
    private Http11NioProtocol protocol;
    @Setter
    private KeyStore keyStore;
    @Setter
    private KeyStore trustStore;
    @Value("${adp-certificate.discovery.password}")
    private String password;
    @Value("${adp-certificate.discovery.keyPassword}")
    private String keyPassword;

    @Autowired
    private SslBundleRegistry sslBundleRegistry;

    @PostConstruct
    public void init() {
        setTrustStore(initializeKeystore(this.password));
        setKeyStore(initializeKeystore(this.password));
        sslBundleRegistry.registerBundle(CertificateId.SERVER.getAlias(), SslBundle.of(this));
    }

    public boolean setContext(TlsContext tlsContext) {
        AtomicBoolean contextUpdated = new AtomicBoolean(false);
        LOGGER.debug("Checking application SSL context with new keystore and truststore for change in alias: {}", tlsContext.getName());
        Optional<TrustStoreItem> tlsContextTrustStore = tlsContext.getTrustStore();
        Optional<KeyStoreItem> tlsContextKeyStore = tlsContext.getKeyStore();

        // Add/Initialize Keystore with keypairs
        tlsContextKeyStore
                .filter(keyStoreItem -> Objects.nonNull(keyStoreItem.getKeyStore()))
                .ifPresent(keyStoreItem -> setKeyStore(keyStoreItem.getKeyStore(), contextUpdated, tlsContext.getName()));
        // Add/Initialize Truststore with CA's
        tlsContextTrustStore
                .filter(trustStoreItem -> Objects.nonNull(trustStoreItem.getTrustStore()))
                .ifPresent(trustStoreItem -> setTrustStore(trustStoreItem.getTrustStore(), contextUpdated, tlsContext.getName()));
        // Add/Initialize Truststore with Client certs
        tlsContextKeyStore
                .filter(keyStoreItem -> Objects.nonNull(keyStoreItem.getKeyStore()))
                .ifPresent(keyStoreItem -> setTrustStore(keyStoreItem.getKeyStore(), contextUpdated));

        return contextUpdated.get();
    }

    private void setKeyStore(KeyStore keyStore, AtomicBoolean contextUpdated, String certificateId) {
        boolean updated = addKeystoreKeyPair(this.keyStore, keyStore, certificateId, this.keyPassword);
        contextUpdated.compareAndSet(false, updated);
    }

    private void setTrustStore(KeyStore trustStore, AtomicBoolean contextUpdated, String... certificateId) {
        boolean updated = addTruststoreCerts(this.trustStore, trustStore, certificateId);
        contextUpdated.compareAndSet(false, updated);
    }

    @Override
    public KeyStore getKeyStore() {
        return this.keyStore;
    }

    @Override
    public String getKeyStorePassword() {
        return this.password;
    }

    @Override
    public KeyStore getTrustStore() {
        return this.trustStore;
    }
}
