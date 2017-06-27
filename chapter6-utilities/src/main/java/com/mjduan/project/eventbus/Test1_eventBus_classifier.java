package com.mjduan.project.eventbus;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.event.japi.LookupEventBus;

import lombok.Getter;
import lombok.Setter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Hans 2017-06-27 21:52
 */
public class Test1_eventBus_classifier {
    private ActorSystem actorSystem;

    @Before
    public void before() {
        actorSystem = ActorSystem.create();
    }

    @After
    public void after(){
        actorSystem.terminate();
    }

    @Test
    public void test1() {
        ActorRef actorRef1 = actorSystem.actorOf(SubscriberActor1.props());
        ActorRef actorRef2 = actorSystem.actorOf(SubscriberActor2.props());
        ActorRef actorRef3 = actorSystem.actorOf(SubscriberActor3.props());
        LookupBusImpl lookupBus = LookupBusImpl.instance();
        lookupBus.subscribe(actorRef1, "greetings");
        lookupBus.subscribe(actorRef2, "greetings");
        lookupBus.subscribe(actorRef3, "foo");

        lookupBus.publish(new MsgEnvelope("'no actor receive this message'", System.currentTimeMillis()));
        lookupBus.publish(new MsgEnvelope("greetings", "'two actor should receive this message'"));
        lookupBus.publish(new MsgEnvelope("foo","'actor3 should receive this message'"));

    }

    public static final class SubscriberActor1 extends AbstractActor {
        private final LoggingAdapter LOG = Logging.getLogger(this);

        public static Props props() {
            return Props.create(SubscriberActor1.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .matchAny(any -> LOG.info("SubscriberActor1 rec {}", any))
                    .build();
        }
    }


    public static final class SubscriberActor2 extends AbstractActor {
        private final LoggingAdapter LOG = Logging.getLogger(this);

        public static Props props() {
            return Props.create(SubscriberActor2.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .matchAny(any -> LOG.info("SubscriberActor2 rec {}", any))
                    .build();
        }
    }

    public static final class SubscriberActor3 extends AbstractActor {
        private final LoggingAdapter LOG = Logging.getLogger(this);

        public static Props props() {
            return Props.create(SubscriberActor3.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .matchAny(any -> LOG.info("SubscriberActor3 rec {}", any))
                    .build();
        }
    }

    @Setter
    @Getter
    public static final class MsgEnvelope {
        private final String topic;
        private final Object payload;

        public MsgEnvelope(String topic, Object payload) {
            this.topic = topic;
            this.payload = payload;
        }
    }

    public static class LookupBusImpl extends LookupEventBus<MsgEnvelope, ActorRef, String> {

        public static LookupBusImpl instance() {
            return new LookupBusImpl();
        }

        @Override
        public int mapSize() {
            return 128;
        }

        @Override
        public int compareSubscribers(ActorRef actorRef, ActorRef s1) {
            return actorRef.compareTo(s1);
        }

        @Override
        public String classify(MsgEnvelope msgEnvelope) {
            return msgEnvelope.topic;
        }

        @Override
        public void publish(MsgEnvelope msgEnvelope, ActorRef actorRef) {
            actorRef.tell(msgEnvelope.payload, ActorRef.noSender());
        }
    }

}
