package com.mjduan.project;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorSystem;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import akka.dispatch.OnSuccess;
import akka.pattern.Patterns;

import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import org.junit.Before;
import org.junit.Test;

/**
 * Hans  2017-06-27 20:40
 */
public class Test5_after {
    private ActorSystem actorSystem;

    @Before
    public void before() {
        actorSystem = ActorSystem.create();
    }

    //it's seems this method doesn't work
    @Test
    public void test1() {
        Future<String> failedFuture = Futures.failed(new IllegalStateException("xxx"));
        Future<String> deplayed = Patterns.after(Duration.create(200, TimeUnit.MILLISECONDS),
                actorSystem.scheduler(), actorSystem.dispatcher(), failedFuture);

        Future<String> future = Futures.future(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Thread.sleep(1_000);
                return "foo";
            }
        }, actorSystem.dispatcher());

        Future<String> firstCompletedOf = Futures.firstCompletedOf(Arrays.<Future<String>>asList(future, deplayed), actorSystem.dispatcher());

        firstCompletedOf.onSuccess(new OnSuccess<String>() {
            @Override
            public void onSuccess(String s) throws Throwable {
                System.out.println("result:" + s);
            }
        }, actorSystem.dispatcher());

        firstCompletedOf.onComplete(new OnComplete<String>() {
            @Override
            public void onComplete(Throwable throwable, String s) throws Throwable {
                System.out.println("s");
            }
        },actorSystem.dispatcher());
    }
}
