package com.mjduan.project.example5_buffers;

import java.util.Arrays;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import org.junit.Test;

/**
 * Hans  2017-06-26 22:19
 */
public class Test1 {
    private ActorMaterializer materializer = ActorMaterializer.create(ActorSystem.create());

    @Test
    public void test(){
        Source.from(Arrays.asList(1,2,3))
                .map(i->{
                    System.out.println("A:"+i);
                    return i;
                }).async()
                .map(i->{
                    System.out.println("B:"+i);
                    return i;
                }).async()
                .map(i->{
                    System.out.println("C:"+i);
                    return i;
                }).async()
                .runWith(Sink.ignore(),materializer);
    }

}
