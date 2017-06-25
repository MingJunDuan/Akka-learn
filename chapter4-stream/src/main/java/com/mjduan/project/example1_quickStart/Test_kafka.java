package com.mjduan.project.example1_quickStart;

import java.util.concurrent.CompletionStage;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.kafka.ProducerSettings;
import akka.kafka.javadsl.Producer;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Source;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;

/**
 * Hans  2017-06-26 00:45
 */
public class Test_kafka {
    private ActorSystem actorSystem = ActorSystem.create();
    private ActorMaterializer materializer = ActorMaterializer.create(actorSystem);

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

}
