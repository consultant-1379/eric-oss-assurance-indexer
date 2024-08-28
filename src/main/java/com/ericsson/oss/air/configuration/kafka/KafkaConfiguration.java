
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
package com.ericsson.oss.air.configuration.kafka;

import com.ericsson.adp.security.certm.certificatewatcher.CertificateWatcherProperties;
import com.ericsson.oss.air.security.config.SecurityProperties;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.Properties;

import static com.ericsson.oss.air.security.config.CertificateId.*;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.*;

@Configuration
@Slf4j
public class KafkaConfiguration {

    @Bean
    public Properties consumerProps(KafkaProperties kafkaProperties, CertificateWatcherProperties certificateWatcherProperties,
                                    SecurityProperties securityProperties) {
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getGroupId());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, kafkaProperties.getEnableAutoCommitConfig());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaProperties.getAutoOffsetResetConfig());
        consumerProps.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, kafkaProperties.getSchemaRegistryUrl());
        if (securityProperties.getTls().getEnabled()) {
            consumerProps.put(SECURITY_PROTOCOL_CONFIG, "SSL");
            final String generatedSslPath = certificateWatcherProperties.getDiscovery().getRootWritePath();
            consumerProps.put(SSL_KEYSTORE_LOCATION_CONFIG, generatedSslPath + File.separator + KAFKA.getAlias() + File.separator + "keystore.p12");
            consumerProps.put(SSL_KEYSTORE_PASSWORD_CONFIG, certificateWatcherProperties.getDiscovery().getPassword());
            consumerProps.put(SSL_KEYSTORE_TYPE_CONFIG, securityProperties.getTls().getKeystoreType());
            consumerProps.put(SSL_KEY_PASSWORD_CONFIG, certificateWatcherProperties.getDiscovery().getKeyPassword());
            consumerProps.put(SSL_TRUSTSTORE_LOCATION_CONFIG, generatedSslPath + File.separator + ROOT.getAlias() + File.separator + "truststore.p12");
            consumerProps.put(SSL_TRUSTSTORE_PASSWORD_CONFIG, certificateWatcherProperties.getDiscovery().getPassword());

            // for Schema Registry
            consumerProps.put("schema.registry.ssl.truststore.location", generatedSslPath + File.separator + ROOT.getAlias() + File.separator + "truststore.p12");
            consumerProps.put("schema.registry.ssl.truststore.password", certificateWatcherProperties.getDiscovery().getPassword());
            consumerProps.put("schema.registry.ssl.keystore.location", generatedSslPath + File.separator + SERVER.getAlias() + File.separator + "keystore.p12" ) ;
            consumerProps.put("schema.registry.ssl.keystore.password", certificateWatcherProperties.getDiscovery().getPassword());

        }
        return consumerProps;
    }
}
