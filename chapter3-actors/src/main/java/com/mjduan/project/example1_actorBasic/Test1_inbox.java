package com.mjduan.project.example1_actorBasic;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import org.junit.Before;
import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;

/**
 * Hans  2017-06-23 19:51
 */
public class Test1_inbox {
    private ActorSystem actorSystem;
    private ActorRef targetActor;

    @Before
    public void before() {
        actorSystem = ActorSystem.create("mySystem");
        targetActor = actorSystem.actorOf(Test1TargetActor.props());
    }

    @Test
    public void test() {
        final Inbox inbox = Inbox.create(actorSystem);
        inbox.send(targetActor, "hello");

        try {
            Object receive = inbox.receive(FiniteDuration.create(1, TimeUnit.SECONDS));
            assert receive.equals("world");
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

    }

    public static final class Test1TargetActor extends AbstractActor {
        private LoggingAdapter LOG = Logging.getLogger(this);

        public static Props props() {
            return Props.create(Test1TargetActor.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .matchAny(any -> processAny(any))
                    .build();
        }

        private void processAny(Object any) {
            LOG.info("Rec {}", any);
            getSender().tell("world", self());
        }


    }

}
