package com.mjduan.project.eventbus;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.testkit.javadsl.TestKit;

import org.junit.Before;
import org.junit.Test;

/**
 * Hans 2017-06-27 22:52
 */
public class Test4_eventstream {
    private ActorSystem actorSystem;

    @Before
    public void before(){
        actorSystem = ActorSystem.create();
    }

    @Test
    public void test_1(){
        TestKit testKit = new TestKit(actorSystem);
        ActorRef actorRef = actorSystem.actorOf(DeadLetterActor.props());
        actorSystem.eventStream().subscribe(actorRef,DeadLetter.class);

        ActorRef ref = testKit.getRef();
        ref.tell("msg1",ActorRef.noSender());
        ref.tell(PoisonPill.getInstance(),ActorRef.noSender());
        ref.tell("msg2",ActorRef.noSender());
        ref.tell("msg3",ActorRef.noSender());
    }

    public static final class DeadLetterActor extends AbstractActor{
        private final LoggingAdapter LOG = Logging.getLogger(this);

        public static Props props(){
            return Props.create(DeadLetterActor.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(DeadLetter.class,msg->{
                        LOG.info("*********DeadLetterActor rec:{}",msg);
                    })
                    .build();
        }
    }
}
