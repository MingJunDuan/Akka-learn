package com.mjduan.project.example3_supervision;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import akka.pattern.Patterns;
import akka.testkit.TestProbe;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import org.junit.Test;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;
import static akka.actor.SupervisorStrategy.resume;
import static akka.actor.SupervisorStrategy.stop;

/**
 * Hans  2017-06-24 22:34
 */
public class Test1_supervisionExample {

    @Test
    public void test() throws Exception {
        ActorSystem actorSystem = ActorSystem.create("faultTolerance");

        FiniteDuration timeout = Duration.create(5, TimeUnit.SECONDS);
        ActorRef supervisor = actorSystem.actorOf(Supervisor.props());
        Future<Object> ask = Patterns.ask(supervisor, Child.props(), 5_000);
        ActorRef child = (ActorRef) Await.result(ask, timeout);


        child.tell(43, ActorRef.noSender());
        assert Await.result(Patterns.ask(child, "get", 3_000), timeout).equals(43);
        child.tell(new ArithmeticException(), ActorRef.noSender());
        assert Await.result(Patterns.ask(child, "get", 3_000), timeout).equals(43);

        child.tell(new NullPointerException(), ActorRef.noSender());
        assert Await.result(Patterns.ask(child, "get", 3_000), timeout).equals(0);


        TestProbe testProbe = new TestProbe(actorSystem);
        testProbe.watch(child);
        child.tell(new IllegalArgumentException(), ActorRef.noSender());
        testProbe.expectMsgClass(Terminated.class);
    }

    @Test
    public void test_sendException() throws Exception {
        ActorSystem actorSystem = ActorSystem.create("faultTolerance");

        FiniteDuration timeout = Duration.create(5, TimeUnit.SECONDS);
        ActorRef supervisorActor = actorSystem.actorOf(Supervisor.props());
        ActorRef childActor = (ActorRef) Await.result(Patterns.ask(supervisorActor, Child.props(), 5_000), timeout);
        childActor.tell(27, ActorRef.noSender());
        assert Await.result(Patterns.ask(childActor, "get", 2_000), timeout).equals(27);

        childActor.tell(new Exception(), ActorRef.noSender());
        assert Await.result(Patterns.ask(childActor, "get", 5_000), timeout).equals(0);
    }

    public static final class Child extends AbstractActor {
        private final LoggingAdapter LOG = Logging.getLogger(this);
        private int state = 0;

        public static Props props() {
            return Props.create(Child.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(Exception.class, e -> {
                        LOG.info("Rec exception ", e.getClass().getName());
                        throw e;
                    })
                    .match(Integer.class, i -> state = i)
                    .matchEquals("get", s -> getSender().tell(state, self()))
                    .build();

        }

    }

    public static final class Supervisor extends AbstractActor {
        private static SupervisorStrategy strategy = new OneForOneStrategy(10, Duration.create(1, TimeUnit.MINUTES),
                DeciderBuilder
                        .match(ArithmeticException.class, e -> resume())
                        .match(NullPointerException.class, e -> restart())
                        .match(IllegalArgumentException.class, e -> stop())
                        .matchAny(any -> escalate())
                        .build());

        public static Props props() {
            return Props.create(Supervisor.class);
        }

        @Override
        public SupervisorStrategy supervisorStrategy() {
            return strategy;
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(Props.class, props -> getSender().tell(getContext().actorOf(props), getSelf()))
                    .build();
        }

        @Override
        public void preRestart(Throwable reason, Optional<Object> message) throws Exception {
            //Do not kill all children, so override this method
        }
    }

}
