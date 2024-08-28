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

import com.ericsson.adp.security.certm.certificatewatcher.CertificateWatcherProperties;
import com.ericsson.oss.air.security.CustomSslStoreBundle;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.Objects;

@Configuration
@ConditionalOnProperty(value = "security.tls.enabled", havingValue = "true")
public class ServerConfiguration {

    @Bean
    @DependsOn("certificateSubscription")
    public TomcatServletWebServerFactory servletContainer(SecurityProperties securityProperties,
                                                          SslBundles sslBundles,
                                                          ServerProperties serverProperties,
                                                          CertificateWatcherProperties certWatcherProperties) {
        final TomcatServletWebServerFactory servletWebServerFactory = new TomcatServletWebServerFactory();
		CustomSslStoreBundle storeBundle = (CustomSslStoreBundle) sslBundles.getBundle(CertificateId.SERVER.getAlias()).getStores();
        servletWebServerFactory.addProtocolHandlerCustomizers(protocolHandler ->
                storeBundle.setProtocol((Http11NioProtocol) protocolHandler));
        if (Objects.nonNull(serverProperties.getPort())) {
            final Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            connector.setPort(serverProperties.getPort());
            servletWebServerFactory.addAdditionalTomcatConnectors(connector);
        }
		
        servletWebServerFactory.setPort(securityProperties.getTls().getPort());
        final Ssl ssl = createSsl(securityProperties, certWatcherProperties, CertificateId.SERVER.getAlias());
        servletWebServerFactory.setSsl(ssl);
        servletWebServerFactory.setSslBundles(sslBundles);
        return servletWebServerFactory;
    }

    private static Ssl createSsl(SecurityProperties securityProperties, CertificateWatcherProperties certificateWatcherProperties, String alias) {
        final String storePass = certificateWatcherProperties.getDiscovery().getPassword();
        final String keyPass = certificateWatcherProperties.getDiscovery().getKeyPassword();

        Ssl ssl = Ssl.forBundle(alias);
        ssl.setEnabled(true);
        ssl.setKeyStorePassword(storePass);
        ssl.setKeyPassword(keyPass);
        ssl.setKeyStoreType(securityProperties.getTls().getKeystoreType());
        ssl.setKeyAlias(alias);
        ssl.setTrustStorePassword(storePass);
        ssl.setClientAuth(securityProperties.getTls().getClientAuth());
        return ssl;
    }
}
