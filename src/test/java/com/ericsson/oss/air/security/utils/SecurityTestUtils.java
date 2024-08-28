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

import com.ericsson.adp.security.certm.certificatewatcher.CertificateWatcherProperties;
import com.ericsson.adp.security.certm.certificatewatcher.KeyStoreItem;
import com.ericsson.adp.security.certm.certificatewatcher.TlsContext;
import com.ericsson.adp.security.certm.certificatewatcher.TrustStoreItem;
import com.ericsson.oss.air.security.config.SecurityProperties;
import lombok.experimental.UtilityClass;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.SSLContext;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.assertj.core.api.iterable.ThrowingExtractor;

import java.nio.file.Path;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@UtilityClass
public class SecurityTestUtils {
    public static final String SERVER_ALIAS = "server";
    private static final String SN = "C=SE,ST=Stockholm,L=Stockholm,OU=IT SERVICES,O=Ericsson,CN=%s";
    public static final String SN_SERVER = String.format(SN, "server");
    public static final String SN_ROOT = String.format(SN, "local root CA");
    public static final String SN_CLIENT = String.format(SN, "client");

    private static SSLContext getSslContext(SSLHostConfig hostConfig) {
        return hostConfig.getCertificates().stream().findFirst().orElseThrow().getSslContext();
    }
	
	public static Enumeration<String> getAliasesFromConfigKeystore(SSLHostConfig hostConfig) {
        try {
            return hostConfig.getCertificates().stream().findFirst().orElseThrow().getCertificateKeystore().aliases();
        } catch (IOException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    public static SSLHostConfig getSslHostConfig(Http11NioProtocol protocol) {
        Stream<SSLHostConfig> sslHostConfigStream = Arrays.stream(protocol.findSslHostConfigs());
        return sslHostConfigStream
                .filter(hostConfig -> hostConfig.getHostName().equals("_default_"))
                .findFirst()
                .orElseThrow();
    }

    public static Function<SSLHostConfig, X509Certificate[]> getClientCertsFunction() {
        return hostConfig -> getSslContext(hostConfig).getAcceptedIssuers();
    }

    public static Function<SSLHostConfig, X509Certificate[]> getServerCertChainFunction(String alias) {
        return hostConfig -> getSslContext(hostConfig).getCertificateChain(alias);
    }

    private static String getSubject(X509Certificate cert) {
        return cert.getSubjectX500Principal().getName();
    }

    private static String getIssuer(X509Certificate cert) {
        return cert.getIssuerX500Principal().getName();
    }

    public static void assertCert(X509Certificate[] x509Certificates, boolean isSubject, String... values) {
        ThrowingExtractor<X509Certificate, String, RuntimeException> extractor = cert -> isSubject ? getSubject(cert) : getIssuer(cert);

        assertThat(x509Certificates)
                .hasSize(values.length)
                .extracting(extractor)
                .containsExactlyInAnyOrder(values);
    }

    public static TlsContext createTlsContext(String storePath,
                                       CertificateWatcherProperties certificateWatcherProperties,
                                       SecurityProperties securityProperties) {
        Path keystorePath = Path.of(storePath + "/keystore.p12");
        Path truststorePath = Path.of(storePath + "/truststore.p12");
        String password = certificateWatcherProperties.getDiscovery().getPassword();
        KeyStoreItem keyStoreItem = KeyStoreItem.builder()
                .path(keystorePath)
                .keyStoreType(securityProperties.getTls().getKeystoreType())
                .password(password)
                .keyPassword(password).build();
        TrustStoreItem trustStoreItem = TrustStoreItem.builder()
                .path(truststorePath)
                .keyStoreType(securityProperties.getTls().getKeystoreType())
                .password(password).build();
        return TlsContext.builder()
                .keyStore(keyStoreItem)
                .trustStore(trustStoreItem)
                .name(SERVER_ALIAS).build();
    }











}
