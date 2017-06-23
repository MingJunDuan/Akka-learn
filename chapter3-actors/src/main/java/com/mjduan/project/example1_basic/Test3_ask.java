package com.mjduan.project.example1_basic;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.PatternsCS;
import akka.util.Timeout;

import scala.concurrent.duration.Duration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.junit.Before;
import org.junit.Test;

/**
 * Hans  2017-06-24 01:12
 */
public class Test3_ask {
    private ActorSystem actorSystem;

    @Before
    public void before() {
        actorSystem = ActorSystem.create("mySystem");
    }

    @Test
    public void test() {
        Timeout timeout = new Timeout(Duration.create(5, TimeUnit.SECONDS));

        ActorRef actor1 = actorSystem.actorOf(Test3Actor1.props());
        ActorRef actor2 = actorSystem.actorOf(Test3Actor2.props());
        ActorRef actor3 = actorSystem.actorOf(Test3Actor3.props());

        CompletableFuture<Object> future1 = PatternsCS.ask(actor1, "Hello actor1", 1_000).toCompletableFuture();
        CompletableFuture<Object> future2 = PatternsCS.ask(actor2, "Hello actor2", timeout).toCompletableFuture();
        CompletableFuture<Test3Actor3.Result> future = CompletableFuture.allOf(future1, future2)
                .thenApply(v -> {
                    String x = (String) future1.join();
                    String y = (String) future2.join();
                    return new Test3Actor3.Result(x + y);
                });
        PatternsCS.pipe(future, actorSystem.dispatcher()).to(actor3);
    }

    public static final class Test3Actor3 extends AbstractActor {
        private final LoggingAdapter LOG = Logging.getLogger(this);

        public static Props props() {
            return Props.create(Test3Actor3.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(Result.class, r -> LOG.info("Rec result {}", r))
                    .build();
        }

        @Setter
        @Getter
        @ToString
        public static final class Result {
            final String result;

            public Result(String result) {
                this.result = result;
            }
        }

    }


    public static final class Test3Actor1 extends AbstractActor {

        public static Props props() {
            return Props.create(Test3Actor1.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(String.class, msg -> {
                        getSender().tell(msg + " actorA", self());
                    })
                    .build();
        }
    }


    public static final class Test3Actor2 extends AbstractActor {

        public static Props props() {
            return Props.create(Test3Actor2.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(String.class, msg -> {
                        getSender().tell(msg + " actorB", self());
                    })
                    .build();
        }
    }
}
