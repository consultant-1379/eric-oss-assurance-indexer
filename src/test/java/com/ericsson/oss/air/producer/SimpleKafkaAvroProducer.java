/*******************************************************************************
 * COPYRIGHT Ericsson 2021
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

package com.ericsson.oss.air.producer;
        import io.confluent.kafka.serializers.KafkaAvroSerializer;
        import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
        import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy;
        import org.apache.avro.Schema;
        import org.apache.avro.generic.GenericRecord;
        import org.apache.avro.generic.GenericRecordBuilder;
        import org.apache.kafka.clients.producer.KafkaProducer;
        import org.apache.kafka.clients.producer.ProducerConfig;
        import org.apache.kafka.clients.producer.ProducerRecord;
        import org.apache.kafka.common.serialization.StringSerializer;
        import java.io.File;
        import java.io.IOException;
        import java.util.Properties;

public class SimpleKafkaAvroProducer {

    public static void main(final String[] args) throws IOException {

        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        properties.put(KafkaAvroSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY, TopicRecordNameStrategy.class.getName());
        properties.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        // Note: we need an active schema registry to use this feature!
        //properties.put("schema.registry.url", "http://localhost:8081");

        KafkaProducer<String, GenericRecord> producer = new KafkaProducer<>(properties);

        var schema1 = new Schema.Parser().parse(new File("./src/test/resources/avro-schemas/schema1.avsc"));
        var schema2 = new Schema.Parser().parse(new File("./src/test/resources/avro-schemas/schema2.avsc"));

        for (int i = 0; i < 5; i++) {
            var arecord = new GenericRecordBuilder(schema1)
                    .set("c1", "context 1...")
                    .set("c2", "Context"+i+":context value...")
                    .set("rvi", i + 100.1)
                    .set("rvf", 100.2 - i)
                    .set("ri1", "info 1:" + i )
                    .set("ri2", "info 2:" + i)
                    .build();
            System.out.println("   === AVRO RECORD ===>   " + arecord);
            ProducerRecord<String, GenericRecord> record = new ProducerRecord<>("kafka-topic", null, arecord);
            producer.send(record);
            //ensures record is sent before closing the producer
        }
        for (int i = 0; i < 5; i++) {
            var arecord = new GenericRecordBuilder(schema2)
                    .set("c1", "schema2 1...")
                    .set("c2", "Context"+i+":context value...")
                    .set("rv1", i + 100.1)
                    .set("rv2", 100.2 - i)
                    .set("ri1", "info 1:" + i )
                    .set("ri2", "info 2:" + i)
                    .build();
            System.out.println("   === AVRO RECORD ===>   " + arecord);
            ProducerRecord<String, GenericRecord> record = new ProducerRecord<>("kafka-topic", null, arecord);
            producer.send(record);
            //ensures record is sent before closing the producer
        }
        producer.flush();
        producer.close();
    }
}