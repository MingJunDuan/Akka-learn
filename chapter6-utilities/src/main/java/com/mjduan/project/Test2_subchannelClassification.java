package com.mjduan.project;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.event.japi.SubchannelEventBus;
import akka.util.Subclassification;

import lombok.Getter;
import lombok.Setter;
import org.junit.Before;
import org.junit.Test;

/**
 * Hans 2017-06-27 22:17
 */
public class Test2_subchannelClassification {
    private ActorSystem actorSystem;

    @Before
    public void before() {
        actorSystem = ActorSystem.create();
    }

    @Test
    public void test1() {
        ActorRef subscriber1 = actorSystem.actorOf(SubscriberActor1.props());
        SubchannelBusImpl subchannelBus = new SubchannelBusImpl();
        subchannelBus.subscribe(subscriber1, "abc");

        subchannelBus.publish(new MsgEnvelope("xyzabc","no one actor should receive this message"));
        subchannelBus.publish(new MsgEnvelope("bcdef","no actor should receive this msg"));
        subchannelBus.publish(new MsgEnvelope("abc","this message should been received"));

        subchannelBus.publish(new MsgEnvelope("abcdef","This msg should been received"));

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

    public static final class SubscriberActor1 extends AbstractActor {
        private final LoggingAdapter LOG = Logging.getLogger(this);

        public static Props props() {
            return Props.create(SubscriberActor1.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .matchAny(any -> LOG.info("SubscriberActor1 rec: {}", any))
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
                    .matchAny(any -> LOG.info("SubscriberActor2 rec: {}", any))
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
                    .matchAny(any -> LOG.info("SubscriberActor3 rec: {}", any))
                    .build();
        }
    }

    public static class StartsWithSubclassification implements Subclassification<String> {

        @Override
        public boolean isEqual(String s, String k1) {
            return s.equals(k1);
        }

        @Override
        public boolean isSubclass(String s, String k1) {
            return s.startsWith(k1);
        }
    }

    public static class SubchannelBusImpl extends SubchannelEventBus<MsgEnvelope, ActorRef, String> {

        @Override
        public Subclassification<String> subclassification() {
            return new StartsWithSubclassification();
        }

        @Override
        public String classify(MsgEnvelope msgEnvelope) {
            return msgEnvelope.getTopic();
        }

        @Override
        public void publish(MsgEnvelope msgEnvelope, ActorRef actorRef) {
            actorRef.tell(msgEnvelope.getPayload(), ActorRef.noSender());
        }
    }
}
