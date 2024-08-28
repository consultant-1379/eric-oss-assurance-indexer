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

import com.ericsson.oss.air.security.utils.exceptions.InternalRuntimeException;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility Class for Keystore operations
 */
@UtilityClass
public class KeystoreUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeystoreUtil.class);

    public static KeyStore initializeKeystore(String keyStorePass) {
        final KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, keyStorePass.toCharArray());
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
            throw new InternalRuntimeException("Failed to initialize keystore.", e);
        }
        return keyStore;
    }

    public static boolean addTruststoreCerts(KeyStore trustStoreCurrent, KeyStore trustStore, String... certificateId) {
        AtomicBoolean trustStoreUpdated = new AtomicBoolean(false);
        String generatedAlias = "";
        try {
            Map<String, Certificate> certificates = getCertEntries(trustStore);
            for (Map.Entry<String, Certificate> entry : certificates.entrySet()) {
                String alias = entry.getKey();
                generatedAlias = createAlias(alias, certificateId);
                Certificate certificate = entry.getValue();
                setCertEntry(trustStoreCurrent, generatedAlias, certificate, trustStoreUpdated);
            }
        } catch (KeyStoreException e) {
            throw new InternalRuntimeException(String.format("Failed to add certificates for alias: %s", generatedAlias), e);
        }
        return trustStoreUpdated.get();
    }

    private static Map<String, Certificate> getCertEntries(KeyStore trustStore) throws KeyStoreException {
        Enumeration<String> aliases = trustStore.aliases();
        Map<String, Certificate> certificates = new HashMap<>();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate certificate = trustStore.getCertificate(alias);
            certificates.put(alias, certificate);
        }
        return certificates;
    }

    private static void setCertEntry(KeyStore trustStoreCurrent, String alias, Certificate certificate, AtomicBoolean trustStoreUpdated) {
        try {
            if (certificateNoneMatch(certificate, getCertEntries(trustStoreCurrent).values())) {
                trustStoreCurrent.setCertificateEntry(alias, certificate);
                trustStoreUpdated.set(true);
            }
            logCertAddedMessage(trustStoreUpdated.get(), (X509Certificate) certificate, alias);
        } catch (KeyStoreException e) {
            throw new InternalRuntimeException(String.format("Failed to add certificate for alias: %s", alias), e);
        }
    }

    private static boolean certificateNoneMatch(Certificate certificate, Collection<Certificate> certificatesCurrent) {
        return certificatesCurrent.stream().noneMatch(certificateCurrent -> {
            try {
                return Arrays.equals(certificateCurrent.getEncoded(), certificate.getEncoded());
            } catch (CertificateEncodingException e) {
                throw new InternalRuntimeException(e.getMessage(), e);
            }
        });
    }

    private static String createAlias(String alias, String... args) {
        ArrayList<String> aliasArgs = new ArrayList<>(List.of(args));
        aliasArgs.add(alias);
        return StringUtils.collectionToDelimitedString(aliasArgs, "-");
    }

    public static boolean addKeystoreKeyPair(KeyStore keyStoreCurrent, KeyStore keyStore, String alias, String keyPassword) {
        boolean shouldSet;
        try {
            Certificate[] certificateChainCurrent = keyStoreCurrent.getCertificateChain(alias);
            Certificate[] certificateChain = keyStore.getCertificateChain(alias);
            KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(keyPassword.toCharArray());
            KeyStore.Entry keyStoreEntry = keyStore.getEntry(alias, protectionParameter);
            boolean match = Arrays.equals(certificateChainCurrent, certificateChain);
            shouldSet = Objects.isNull(certificateChainCurrent) || !match;
            if (shouldSet) {
                keyStoreCurrent.setEntry(alias, keyStoreEntry, protectionParameter);
            }
            logCertAddedMessage(shouldSet, ((X509Certificate) ((KeyStore.PrivateKeyEntry) keyStoreEntry).getCertificate()), alias);
        } catch (KeyStoreException | UnrecoverableEntryException | NoSuchAlgorithmException e) {
            throw new InternalRuntimeException(String.format("Failed to add/set keystore key pair for: %s", alias), e);
        }
        return shouldSet;
    }


    private static void logCertAddedMessage(boolean added, X509Certificate certificate, String alias) {
        final String certSkipMsg = "Certificate already present. skip processing..,";
        final String certAddedMsg = "Certificate not found. Signal will be processed and certificate will be added.";
        String message = added ? certAddedMsg : certSkipMsg;
        LOGGER.debug("{} Alias: {}, SN: {}", message, alias, certificate.getSubjectX500Principal());
    }
}
