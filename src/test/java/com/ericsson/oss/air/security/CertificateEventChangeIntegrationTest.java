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

import com.ericsson.oss.air.CoreApplication;
import com.ericsson.oss.air.IndexerDB;
import com.ericsson.oss.air.RecordConsumer;
import com.ericsson.oss.air.security.utils.CertificateEventChangeTestInitializer;
import org.apache.commons.io.FileUtils;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static com.ericsson.oss.air.security.utils.SecurityTestUtils.*;
import static org.apache.tomcat.util.net.SSLHostConfig.CertificateVerification.REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = {CoreApplication.class})
@ContextConfiguration(initializers = CertificateEventChangeTestInitializer.class)
@DirtiesContext
class CertificateEventChangeIntegrationTest extends AbstractTlsTestSetup {
    private static final String SN = "C=SE,ST=Stockholm,L=Stockholm,OU=IT SERVICES,O=Ericsson,CN=%s";
    private static final String SN_SERVER = String.format(SN, "server");
    private static final String SN_SERVER_UPDATED = String.format(SN, "server-updated");
    private static final String SN_ROOT = String.format(SN, "local root CA");
    private static final String SN_CLIENT = String.format(SN, "client");
    private static final String SN_CLIENT_UPDATED = String.format(SN, "client-updated");
    private static final String SN_LOG = String.format(SN, "log");
    private static final String SN_LOG_UPDATED = String.format(SN, "log-updated");
    private static final String SN_LOG_CA = String.format(SN, "local log CA");

    @Autowired
    private SslBundles sslBundles;

    @SpyBean
    private RestTemplateReloader restTemplateReloader;

    @MockBean
    private IndexerDB indexerDB;

    @MockBean
    private RecordConsumer recordConsumer;

    @AfterAll
    public static void cleanup() throws IOException {
        File certDir = ResourceUtils.getFile("target/security");
        FileUtils.forceDelete(certDir);
    }

    @Test
    void checkIfServerCertificateChanges() throws IOException {
        String alias = "server";
        CustomSslStoreBundle sslStoreBundle = (CustomSslStoreBundle) sslBundles.getBundle(alias).getStores();
        Http11NioProtocol protocol = spy(sslStoreBundle.getProtocol());
        sslStoreBundle.setProtocol(protocol);
        SSLHostConfig sslHostConfig = getSslHostConfig(protocol);
        assertThat(protocol.getSecure()).isTrue();
        assertThat(sslHostConfig.getCertificateVerification().name()).isEqualTo(REQUIRED.toString());
        assertThat(Collections.list(getAliasesFromConfigKeystore(sslHostConfig))).contains(alias);
        assertThat(sslHostConfig)
                .extracting(getServerCertChainFunction(alias))
                .satisfies(x509Certificates -> assertCert(x509Certificates, true, SN_SERVER))
                .satisfies(x509Certificates -> assertCert(x509Certificates, false, SN_ROOT));
        assertThat(sslHostConfig)
                .extracting(getClientCertsFunction())
                .satisfies(x509Certificates -> assertCert(x509Certificates, true, SN_ROOT, SN_CLIENT, SN_LOG, SN_SERVER))
                .satisfies(x509Certificates -> assertCert(x509Certificates, false, SN_ROOT, SN_ROOT, SN_LOG_CA, SN_ROOT));

        // When server cert updates, should reload
        File source = ResourceUtils.getFile("src/test/resources/security/server-updated/keystore");
        File dest = new File("target/security/server/keystore");
        FileUtils.copyDirectory(source, dest, false);
        awaitFibonacci().untilAsserted(() -> {
            verify(protocol).reloadSslHostConfig(anyString());
            assertThat(Collections.list(getAliasesFromConfigKeystore(sslHostConfig))).contains(alias);
            assertThat(sslHostConfig)
                    .extracting(getServerCertChainFunction(alias))
                    .satisfies(x509Certificates -> assertCert(x509Certificates, false, SN_ROOT))
                    .satisfies(x509Certificates -> assertCert(x509Certificates, true, SN_SERVER_UPDATED));

            assertThat(sslHostConfig)
                    .extracting(getClientCertsFunction())
                    .satisfies(x509Certificates -> assertCert(x509Certificates, true, SN_ROOT, SN_CLIENT, SN_LOG, SN_SERVER_UPDATED))
                    .satisfies(x509Certificates -> assertCert(x509Certificates, false, SN_ROOT, SN_ROOT, SN_LOG_CA, SN_ROOT));
        });

        // When a file change detected other than the keypair update, should not reload
        File testFile = new File("target/security/server/truststore/test.file");
        assertThat(testFile.createNewFile()).isTrue();
        awaitFibonacci().untilAsserted(() -> {
            verify(protocol, atMostOnce()).reloadSslHostConfig(anyString());
            assertThat(Collections.list(getAliasesFromConfigKeystore(sslHostConfig))).contains(alias);
            assertThat(sslHostConfig)
                    .extracting(getServerCertChainFunction(alias))
                    .satisfies(x509Certificates -> assertCert(x509Certificates, false, SN_ROOT))
                    .satisfies(x509Certificates -> assertCert(x509Certificates, true, SN_SERVER_UPDATED));

            assertThat(sslHostConfig)
                    .extracting(getClientCertsFunction())
                    .satisfies(x509Certificates -> assertCert(x509Certificates, true, SN_ROOT, SN_CLIENT, SN_LOG, SN_SERVER_UPDATED))
                    .satisfies(x509Certificates -> assertCert(x509Certificates, false, SN_ROOT, SN_ROOT, SN_LOG_CA, SN_ROOT));
        });

        // When a new cert added to truststore, should reload
        File cert = ResourceUtils.getFile("src/test/resources/security/server-updated/truststore/clientcert-updated.crt");
        File destCert = new File("target/security/server/truststore/clientcert-updated.crt");
        FileUtils.copyFile(cert, destCert, false);
        awaitFibonacci().untilAsserted(() -> {
            verify(protocol, times(2)).reloadSslHostConfig(anyString());
            assertThat(Collections.list(getAliasesFromConfigKeystore(sslHostConfig))).contains(alias);
            assertThat(sslHostConfig)
                    .extracting(getServerCertChainFunction(alias))
                    .satisfies(x509Certificates -> assertCert(x509Certificates, false, SN_ROOT))
                    .satisfies(x509Certificates -> assertCert(x509Certificates, true, SN_SERVER_UPDATED));

            assertThat(sslHostConfig)
                    .extracting(getClientCertsFunction())
                    .satisfies(x509Certificates ->
                            assertCert(x509Certificates, true, SN_ROOT, SN_CLIENT, SN_CLIENT_UPDATED, SN_LOG, SN_SERVER_UPDATED))
                    .satisfies(x509Certificates -> assertCert(x509Certificates, false, SN_ROOT, SN_ROOT, SN_ROOT, SN_LOG_CA, SN_ROOT));
        });

        // When existing cert added to truststore, should not reload
        cert = ResourceUtils.getFile("src/test/resources/security/server-updated/truststore/clientcert-updated.crt");
        destCert = new File("target/security/server/truststore/clientcert-updated-same.crt");
        FileUtils.copyFile(cert, destCert, false);
        awaitFibonacci().untilAsserted(() -> {
            verify(protocol, atMost(2)).reloadSslHostConfig(anyString());
            assertThat(Collections.list(getAliasesFromConfigKeystore(sslHostConfig))).contains(alias);
            assertThat(sslHostConfig)
                    .extracting(getServerCertChainFunction(alias))
                    .satisfies(x509Certificates -> assertCert(x509Certificates, false, SN_ROOT))
                    .satisfies(x509Certificates -> assertCert(x509Certificates, true, SN_SERVER_UPDATED));

            assertThat(sslHostConfig)
                    .extracting(getClientCertsFunction())
                    .satisfies(x509Certificates ->
                            assertCert(x509Certificates, true, SN_ROOT, SN_CLIENT, SN_CLIENT_UPDATED, SN_LOG, SN_SERVER_UPDATED))
                    .satisfies(x509Certificates -> assertCert(x509Certificates, false, SN_ROOT, SN_ROOT, SN_ROOT, SN_LOG_CA, SN_ROOT));
        });

        // When a client cert edited/replaced, should reload
        cert = ResourceUtils.getFile("src/test/resources/security/log-updated/keystore");
        destCert = new File("target/security/log/keystore");
        FileUtils.copyDirectory(cert, destCert, false);
        awaitFibonacci().untilAsserted(() -> {
            verify(protocol, times(3)).reloadSslHostConfig(anyString());
            assertThat(Collections.list(getAliasesFromConfigKeystore(sslHostConfig))).contains(alias);
            assertThat(sslHostConfig)
                    .extracting(getServerCertChainFunction(alias))
                    .satisfies(x509Certificates -> assertCert(x509Certificates, false, SN_ROOT))
                    .satisfies(x509Certificates -> assertCert(x509Certificates, true, SN_SERVER_UPDATED));

            assertThat(sslHostConfig)
                    .extracting(getClientCertsFunction())
                    .satisfies(x509Certificates ->
                            assertCert(x509Certificates, true, SN_ROOT, SN_CLIENT, SN_CLIENT_UPDATED, SN_LOG_UPDATED, SN_SERVER_UPDATED))
                    .satisfies(x509Certificates -> assertCert(x509Certificates, false, SN_ROOT, SN_ROOT, SN_ROOT, SN_LOG_CA, SN_ROOT));
        });
    }

    private static ConditionFactory awaitFibonacci() {
        return await().pollDelay(Duration.ofSeconds(2)).pollInterval(fibonacci(5, TimeUnit.MILLISECONDS));
    }
}