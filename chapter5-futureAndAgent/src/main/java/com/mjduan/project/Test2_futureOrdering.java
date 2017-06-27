package com.mjduan.project;

import akka.actor.ActorSystem;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;

import org.junit.Before;
import org.junit.Test;

/**
 * Hans  2017-06-27 20:15
 */
public class Test2_futureOrdering {
    private ActorSystem actorSystem;


    @Before
    public void before(){
        actorSystem = ActorSystem.create();
    }

    @Test
    public void test1(){
        Futures.successful("value").andThen(new OnComplete<String>() {
            @Override
            public void onComplete(Throwable throwable, String s) throws Throwable {
                if (throwable != null) {
                    System.out.println("error occur");
                } else {
                    System.out.println("s:"+s);
                }
            }
        },actorSystem.dispatcher()).andThen(new OnComplete<String>() {
            @Override
            public void onComplete(Throwable throwable, String s) throws Throwable {
                System.out.println("second oncmplete");
            }
        },actorSystem.dispatcher());
    }

}
