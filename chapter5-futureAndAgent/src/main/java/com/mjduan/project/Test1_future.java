package com.mjduan.project;

import java.util.Arrays;
import java.util.concurrent.Callable;

import akka.actor.ActorSystem;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import akka.dispatch.OnFailure;
import akka.dispatch.OnSuccess;
import akka.japi.Function;

import scala.concurrent.Future;
import scala.concurrent.Promise;

import org.junit.Before;
import org.junit.Test;

/**
 * Hans  2017-06-27 00:32
 */
public class Test1_future {

    private ActorSystem actorSystem;

    @Before
    public void before() {
        actorSystem = ActorSystem.create("mySystem");
    }

    @Test
    public void test1() {
        Future<String> future = Futures.successful("hello world");

        Promise<String> promise = Futures.promise();
        Future<String> future1 = promise.future();
        promise.success("hello");
    }

    @Test
    public void test2() {
        Future<String> future = Futures.future(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "Hello world";
            }
        }, actorSystem.dispatcher());

        future.onSuccess(new PrintResult<String>(), actorSystem.dispatcher());
    }

    @Test
    public void test3() {
        Future<String> f1 = Futures.future(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "hello" + "world";
            }
        }, actorSystem.dispatcher());

        Future<Integer> f2 = f1.map(new Mapper<String, Integer>() {
            @Override
            public Integer apply(String parameter) {
                return parameter.length();
            }
        }, actorSystem.dispatcher());

        f2.onSuccess(new PrintResult<Integer>(), actorSystem.dispatcher());
    }

    @Test
    public void test_composingFuture() {
        Iterable<Future<Integer>> futureIterator = Arrays.asList(getFutureInteger(1), getFutureInteger(2), getFutureInteger(3));
        Future<Iterable<Integer>> sequence = Futures.sequence(futureIterator, actorSystem.dispatcher());

        Future<Long> futureSum = sequence.map(new Mapper<Iterable<Integer>, Long>() {
            @Override
            public Long apply(Iterable<Integer> ints) {
                long sum = 0;
                for (Integer i : ints) {
                    sum += i;
                }
                return sum;
            }
        }, actorSystem.dispatcher());

        futureSum.onSuccess(new PrintResult<>(), actorSystem.dispatcher());
    }

    @Test
    public void test_4() {
        Iterable<String> stringIterable = Arrays.asList("a", "b", "c");
        Future<Iterable<String>> traverseFuture = Futures.traverse(stringIterable,
                new Function<String, Future<String>>() {
                    @Override
                    public Future<String> apply(String s) throws Exception {
                        return Futures.future(new Callable<String>() {
                            @Override
                            public String call() throws Exception {
                                return s.toUpperCase();
                            }
                        }, actorSystem.dispatcher());
                    }
                }, actorSystem.dispatcher());

        traverseFuture.onSuccess(new PrintResult<>(), actorSystem.dispatcher());
    }

    private Future<Integer> getFutureInteger(final int i) {
        return Futures.future(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return i;
            }
        }, actorSystem.dispatcher());
    }

    @Test
    public void test5() {
        Future<String> future = Futures.successful("helloworld");

        future.onSuccess(new OnSuccess<String>() {
            @Override
            public void onSuccess(String s) throws Throwable {
                System.out.println("onSuccess method:" + s);
            }
        }, actorSystem.dispatcher());

    }

    @Test
    public void test6() {
        Future<String> future = Futures.successful("hello");
        future.onComplete(new OnComplete<String>() {
            @Override
            public void onComplete(Throwable throwable, String s) throws Throwable {
                System.out.println("onComplete:" + s);
            }
        }, actorSystem.dispatcher());

        future.onSuccess(new OnSuccess<String>() {
            @Override
            public void onSuccess(String s) throws Throwable {
                System.out.println("onSuccess:" + s);
            }
        }, actorSystem.dispatcher());
    }

    @Test
    public void test7_onFail() {
        Future<String> future = Futures.future(new Callable<String>() {
            @Override
            public String call() throws Exception {
                throw new NullPointerException("custom null pointer");
            }
        }, actorSystem.dispatcher());

        future.onComplete(new OnComplete<String>() {
            @Override
            public void onComplete(Throwable throwable, String s) throws Throwable {
                if (throwable!=null){
                    System.out.println("error occur");
                }
                System.out.println("onComplete, s=" + s);
            }
        }, actorSystem.dispatcher());

        future.onFailure(new OnFailure() {
            @Override
            public void onFailure(Throwable throwable) throws Throwable {
                System.out.println("onFailure");
                throwable.printStackTrace();
            }
        }, actorSystem.dispatcher());

        future.onSuccess(new OnSuccess<String>() {
            @Override
            public void onSuccess(String s) throws Throwable {
                System.out.println("onSuccess");
            }
        }, actorSystem.dispatcher());
    }

    public static final class PrintResult<T> extends OnSuccess<T> {

        @Override
        public void onSuccess(T t) throws Throwable {
            System.out.println("printResult:" + t);
        }
    }


}
