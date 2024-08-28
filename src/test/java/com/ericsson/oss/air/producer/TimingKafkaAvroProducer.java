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

import com.ericsson.oss.air.services.OpenSearchService;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;



public class TimingKafkaAvroProducer {
    public static String readFileAsString(String file)throws Exception
    {
        return new String(Files.readAllBytes(Paths.get(file)));
    }
    public static void main(final String[] args) throws Exception {

        var indexerUrl = "http://localhost:8080/v1/indexer-info/indexer";
        var requestPath = "src/test/resources/json-files/indexer.json";
        var requestData = readFileAsString(requestPath);

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var request = new HttpEntity<String>(requestData, headers);

        RestTemplate restTemplate = new RestTemplate();

        restTemplate.exchange(indexerUrl,
                HttpMethod.POST,
                request,
                String.class);

        /* ^ This creates the index if not already present. However, there is a bug
         *   somewhere in indexer that requires us to run this file twice on a new
         *   instance of open search for it to work properly.
         *   I do not believe the source of this bug is related to this ticket/test
         *   ie, you must terminate the first process when you first run it and try again for it to work
         */

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

        var httpHost = new org.apache.http.HttpHost("localhost", 9200, "http");
        var restClient = RestClient.builder(httpHost).build();
        var transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        var service = new OpenSearchService(new OpenSearchClient(transport));
        var targetName = "assurance-an-index";
        int dt=0;
        int trials=3;
        int numRecords = 5;

        for(int iter=0; iter<trials;iter++) {
            long timeStart = 0;
            long timeEnd = 0;
            int offset = -1;
            List<ObjectNode> indexers = service.getAllDocs(targetName, ObjectNode.class);
            System.out.println("Start Indexers: " + indexers);
            indexers = indexers
                    .stream()
                    .filter(index -> {var c1 = index.get("c_c1").toString(); return c1 != null && c1.equals("\"KAPTest\"");})
                    .collect(Collectors.toList());
            System.out.println("Filtered Indexers: " + indexers);

            if (indexers.size() > 0){
                if(indexers.size() > 1){
                    System.out.println("*** WARN: Unexpected number of indices ***");
                }
                var firstIndexer = indexers.get(0);
                offset = firstIndexer.get("offset").asInt();
            }

            for (int i = 0; i < numRecords; i++) {
                var arecord = new GenericRecordBuilder(schema2)
                        .set("c1", "KAPTest")
                        .set("c2", "context 2...")
                        .set("rv1", i + 200.1)
                        .set("rv2", 100.2 - i)
                        .set("ri1", "info 1:" + i)
                        .set("ri2", "info 2:" + i)
                        .build();

                System.out.println("   === AVRO RECORD ===>   " + arecord);
                ProducerRecord<String, GenericRecord> record = new ProducerRecord<>("kafka-topic", null, arecord);

                timeStart = System.currentTimeMillis();
                producer.send(record);
                //ensures record is sent before closing the producer
            }
            producer.flush();



            System.out.println(" STARING with offset: " + offset);
            var x=0;
            while(true) {
                x++;
                TimeUnit.MILLISECONDS.sleep(1);
                timeEnd = System.currentTimeMillis();
                indexers = service.getAllDocs(targetName, ObjectNode.class)
                        .stream()
                        .filter(index -> {var c1 = index.get("c_c1").toString(); return c1 != null && c1.equals("\"KAPTest\"");})
                        .collect(Collectors.toList());
                if (indexers.size() > 0) {
                    if(indexers.size() > 1){
                        System.out.println("*** WARN: Unexpected number of indices ***");
                    }
                    System.out.println("----------------------- loop:" + x + " --------------------------\n indexers: " + indexers);
                    var firstIndexer = indexers.get(indexers.size()-1);
                    var newOffSet = firstIndexer.get("offset").asInt();
                    if (newOffSet == offset) {
                        System.out.println("same offset: " + newOffSet + " waiting...");
                    } else {
                        System.out.println("new offset: " + newOffSet);
                        break;
                    }
                }
            }
            dt+=timeEnd-timeStart;
            System.out.println("curDeltaTime: " + (timeEnd-timeStart));
            System.out.println("end");
        }

        producer.close();
        System.out.println("DeltaTime: " + dt);
        System.out.println("avg dt = " + (dt/trials));

    }
}
