package com.mjduan.project;

import java.util.concurrent.Callable;

import akka.actor.ActorSystem;
import akka.dispatch.Futures;
import akka.dispatch.OnSuccess;
import akka.dispatch.Recover;

import scala.concurrent.Future;

import org.junit.Before;
import org.junit.Test;

/**
 * Hans  2017-06-27 20:33
 */
public class Test4_exception {
    private ActorSystem actorSystem;

    @Before
    public void before(){
        actorSystem = ActorSystem.create();
    }

    @Test
    public void test1(){
        Future<Integer> recover = Futures.future(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 1 / 0;
            }
        }, actorSystem.dispatcher())
                .recover(new Recover<Integer>() {
                    @Override
                    public Integer recover(Throwable throwable) throws Throwable {
                        if (throwable instanceof ArithmeticException) {
                            return 0;
                        } else {
                            throw throwable;
                        }
                    }
                }, actorSystem.dispatcher());

        recover.onSuccess(new OnSuccess<Integer>() {
            @Override
            public void onSuccess(Integer integer) throws Throwable {
                System.out.println("result should be 0. The actual value is " + integer);
            }
        },actorSystem.dispatcher());
    }

}
