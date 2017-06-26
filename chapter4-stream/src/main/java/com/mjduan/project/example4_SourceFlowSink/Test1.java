package com.mjduan.project.example4_SourceFlowSink;

import java.util.Arrays;
import java.util.concurrent.CompletionStage;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import org.junit.Before;
import org.junit.Test;

/**
 * Hans  2017-06-26 21:33
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
    public void test_connectSourceSink(){
        Source<Integer, NotUsed> source = Source.from(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        Sink<Integer, CompletionStage<Integer>> sink = Sink.<Integer, Integer>fold(0, (aggre, next) -> aggre + next);

        RunnableGraph<CompletionStage<Integer>> runnable = source.toMat(sink, Keep.right());
        CompletionStage<Integer> completionStage = runnable.run(materializer);

        completionStage.whenComplete((i,e)->System.out.println(i));
    }

    @Test
    public void test_materializationTwice(){
        Sink<Integer, CompletionStage<Integer>> sink = Sink.<Integer, Integer>fold(0, (aggr, next) -> aggr + next);
        RunnableGraph<CompletionStage<Integer>> runnableGraph = Source.from(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
                .toMat(sink, Keep.right());

        CompletionStage<Integer> stage1 = runnableGraph.run(materializer);
        CompletionStage<Integer> stage2 = runnableGraph.run(materializer);

        if (stage1 != stage2) {
            System.out.println("different");
        }
        stage1.whenComplete((i,e)->System.out.println(i));
        stage2.whenComplete((i,e)->System.out.println(i));
    }


}
