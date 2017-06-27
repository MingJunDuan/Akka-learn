package com.mjduan.project;

import akka.actor.ActorSystem;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.dispatch.OnSuccess;

import scala.Tuple2;
import scala.concurrent.Future;

import org.junit.Before;
import org.junit.Test;

/**
 * Hans  2017-06-27 20:21
 */
public class Test3_auxiliary {
    private ActorSystem actorSystem;

    @Before
    public void before() {
        actorSystem = ActorSystem.create();
    }

    @Test
    public void test1() {
        Future<Object> f1 = Futures.failed(new IllegalStateException("xx"));
        Future<Object> f2 = Futures.failed(new IllegalStateException("xxxx"));
        Future<String> f3 = Futures.successful("bar");

        Future<String> f4 = f1.fallbackTo(f2).fallbackTo(f3);
        f4.onSuccess(new OnSuccess<String>() {
            @Override
            public void onSuccess(String s) throws Throwable {
                System.out.println("result:" + s);
            }
        }, actorSystem.dispatcher());
    }

    @Test
    public void test2() {
        Future<String> f1 = Futures.successful("foo");
        Future<String> f2 = Futures.successful("bar");

        Future<Tuple2<String, String>> zip = f1.zip(f2);
        Future<String> map = zip.map(new Mapper<Tuple2<String, String>, String>() {
            @Override
            public String apply(Tuple2<String, String> parameter) {
                String s1 = parameter._1();
                String s2 = parameter._2();
                System.out.println("s1=" + s1 + "\ts2=" + s2);
                return s1 + " " + s2;
            }
        }, actorSystem.dispatcher());
        map.onSuccess(new OnSuccess<String>() {
            @Override
            public void onSuccess(String s) throws Throwable {
                System.out.println("final result =" + s);
            }
        }, actorSystem.dispatcher());
    }
}
