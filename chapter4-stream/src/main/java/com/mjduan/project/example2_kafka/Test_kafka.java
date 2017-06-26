package com.mjduan.project.example2_kafka;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.kafka.ProducerSettings;
import akka.kafka.javadsl.Producer;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Source;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;

/**
 * Hans  2017-06-26 00:45
 */
public class Test_kafka {
    private ActorSystem actorSystem = ActorSystem.create();
    private ActorMaterializer materializer = ActorMaterializer.create(actorSystem);

    //this method doesn't work
    @Test
    public void test_producer() {
        ProducerSettings<byte[], String> producerSettings = ProducerSettings.create(actorSystem, new ByteArraySerializer(), new StringSerializer())
                .withBootstrapServers("localhost:9092");
        KafkaProducer<byte[], String> kafkaProducer = producerSettings.createKafkaProducer();

        Source<ProducerRecord<byte[], String>, NotUsed> record = Source.range(1, 100)
                .map(n -> {
                    System.out.println(n);
                    return n.toString() + " value";
                })
                .map(elem -> new ProducerRecord<byte[], String>("akkaIn", elem));

        CompletionStage<Done> stage = record.runForeach(r -> {
            System.out.println(r.value());
            Producer.plainSink(producerSettings, kafkaProducer);
        }, materializer);
        stage.thenRun(() -> actorSystem.terminate());
    }

    //this method doesn't work
    @Test
    public void test_akkaKafka_producer(){
        ProducerSettings<String, String> producerSettings = ProducerSettings.create(actorSystem, new StringSerializer(), new StringSerializer())
                .withBootstrapServers("localhost:9092");
        CompletionStage<Done> completionStage = Source.range(100, 1000)
                .map(n -> n.toString())
                .map(elem -> new ProducerRecord<String, String>("akkaIn", "key " + elem, "value===" + elem))
                .runWith(Producer.plainSink(producerSettings), materializer);
        completionStage.whenComplete(new BiConsumer<Done, Throwable>() {
            @Override
            public void accept(Done done, Throwable throwable) {
                System.out.println("end");
                //actorSystem.terminate();
            }
        });
    }

    @Test
    public void test_kafkaProducer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("acks", "all");
        properties.put("retries", 0);
        properties.put("batch.size", 16384);
        properties.put("linger.ms", 1);
        properties.put("buffer.memory", 33554432);
        properties.put("key.serializer", StringSerializer.class.getCanonicalName());
        properties.put("value.serializer", StringSerializer.class.getCanonicalName());

        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(properties);
        for (int i = 0; i < 100; i++) {
            kafkaProducer.send(new ProducerRecord<String, String>("akkaIn", "value i=" + i, "value " + i));
        }

        kafkaProducer.close();
    }


    @Test
    public void test_kafkaConsumer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("group.id", "consumerGroup");
        properties.put("enable.auto.commit", "true");
        properties.put("auto.commit.interval.ms", "1000");
        properties.put("session.timeout.ms", "30000");
        properties.put("key.deserializer", StringDeserializer.class.getCanonicalName());
        properties.put("value.deserializer", StringDeserializer.class.getCanonicalName());

        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties);
        kafkaConsumer.subscribe(Arrays.asList("akkaIn"));

        while (true) {
            ConsumerRecords<String, String> records = kafkaConsumer.poll(300);
            for (ConsumerRecord<String, String> record : records) {
                System.out.printf("topic=%s partition=%d offset=%d key=%s value=%s\n",
                        record.topic(), record.partition(), record.offset(), record.key(), record.value());
            }
        }
    }

}
