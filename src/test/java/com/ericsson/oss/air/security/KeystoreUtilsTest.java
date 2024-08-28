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


import com.ericsson.oss.air.security.utils.exceptions.InternalRuntimeException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static com.ericsson.oss.air.security.utils.KeystoreUtil.addTruststoreCerts;
import static com.ericsson.oss.air.security.utils.KeystoreUtil.initializeKeystore;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KeystoreUtilsTest extends AbstractTestSetup {
    @Test
    void shouldInitializeKeystore() throws KeyStoreException {
        KeyStore keystore = initializeKeystore("pass");
        assertThat(keystore).isNotNull();
        assertThat(keystore.size()).isZero();
        assertThat(keystore.getType()).isEqualTo("pkcs12");
    }

    @Test
    void setCertEntryShouldFail() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore truststoreCurrent = mock(KeyStore.class);
        when(truststoreCurrent.aliases()).thenCallRealMethod();
        KeyStore truststore = KeyStore.getInstance(KeyStore.getDefaultType());
        truststore.load(null, "test".toCharArray());
        Certificate certificate = mock(X509Certificate.class);
        truststore.setCertificateEntry("test", certificate);
        assertThatThrownBy(() -> addTruststoreCerts(truststoreCurrent, truststore, "server"))
                .isInstanceOf(InternalRuntimeException.class);
    }

    @Test
    void certificateNoneMatchShouldThrowEncodingException() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore truststore = KeyStore.getInstance(KeyStore.getDefaultType());
        truststore.load(null, "test".toCharArray());
        Certificate certificate = mock(X509Certificate.class);
        truststore.setCertificateEntry("test", certificate);
        when(certificate.getEncoded()).thenThrow(CertificateEncodingException.class);
        assertThatThrownBy(() -> addTruststoreCerts(truststore, truststore, "server"))
                .isInstanceOf(InternalRuntimeException.class);
    }
}
