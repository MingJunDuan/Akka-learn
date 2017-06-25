package com.mjduan.project.example1_quickStart;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.japi.function.Function2;
import akka.japi.function.Procedure;
import akka.stream.ActorMaterializer;
import akka.stream.IOResult;
import akka.stream.Materializer;
import akka.stream.ThrottleMode;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;

import scala.concurrent.duration.Duration;

import org.junit.Test;

/**
 * Hans  2017-06-25 23:43
 */
public class Test_helloStream {
    private ActorSystem actorSystem = ActorSystem.create("quickStartSystem");
    private Materializer materializer = ActorMaterializer.create(actorSystem);


    @Test
    public void test_source() {
        Source<Integer, NotUsed> source = Source.range(1, 100);
        CompletionStage<Done> completionStage = source.runForeach(new Procedure<Integer>() {
            @Override
            public void apply(Integer integer) throws Exception {
                System.out.println(integer);
            }
        }, materializer);

        completionStage.thenRun(() -> actorSystem.terminate());
    }

    @Test
    public void test_map() {
        Source<Integer, NotUsed> source = Source.range(1, 100);
        Source<Integer, NotUsed> sum = source.scan(0, new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer integer, Integer integer2) throws Exception {
                System.out.println("i1=" + integer + "\t i2=" + integer2);
                return integer + integer2;
            }
        });

        CompletionStage<Done> completionStage = sum.runForeach(i -> System.out.println(i), materializer);

        completionStage.thenRun(() -> actorSystem.terminate());
    }

    @Test
    public void test() {
        final String filename = "/tmp/test.log";
        Source<Integer, NotUsed> source = Source.range(1, 50);
        Source<String, NotUsed> usedSource = source.map(i -> i.toString());
        CompletionStage<IOResult> completionStage = usedSource.runWith(lineSink(filename), materializer);

        completionStage.thenRun(() -> actorSystem.terminate());
    }

    //reusable pieces
    private Sink<String, CompletionStage<IOResult>> lineSink(String filename) {
        Sink<String, CompletionStage<IOResult>> completionStageSink = Flow.of(String.class)
                .map(s -> ByteString.fromString("value=" + s.toString() + "\n"))
                .toMat(FileIO.toPath(Paths.get(filename)), Keep.right());
        return completionStageSink;
    }

    @Test
    public void test_timeBasedProcessing() {
        Source<Integer, NotUsed> source = Source.range(1, 100);
        CompletionStage<Done> completionStage = source
                .zipWith(Source.range(0, 99), (num, index) -> String.format(getCurrentTime() + " num %d at index %d", num, index))
                .throttle(1, Duration.create(1, TimeUnit.SECONDS), 1, ThrottleMode.shaping())
                .runForeach(s -> System.out.println(s), materializer);

        //completionStage.thenRun(() -> actorSystem.terminate());
    }

    private String getCurrentTime() {
        final String pattern = "yyyy-MM-dd HH:mm:ss";
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
        return time;
    }

}
