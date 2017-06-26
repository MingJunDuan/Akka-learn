package com.mjduan.project.example3_streamFileIO;

import java.nio.file.Paths;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

import akka.Done;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.IOResult;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Sink;
import akka.util.ByteString;

import org.junit.Before;
import org.junit.Test;

/**
 * Hans  2017-06-26 21:03
 */
public class Test1 {
    private ActorSystem actorSystem;
    private ActorMaterializer materializer;

    @Before
    public void before(){
        actorSystem = ActorSystem.create();
        materializer = ActorMaterializer.create(actorSystem);
    }

    @Test
    public void test_read(){
        Sink<ByteString, CompletionStage<Done>> sink = Sink.<ByteString>foreach(chunk -> System.out.println(chunk.utf8String()));

        CompletionStage<IOResult> stage = FileIO.fromPath(Paths.get("/tmp/test.log"))
                .to(sink)
                .run(materializer);

        stage.whenComplete(new BiConsumer<IOResult, Throwable>() {
            @Override
            public void accept(IOResult ioResult, Throwable throwable) {
                System.out.println("end");
            }
        });
    }
}
