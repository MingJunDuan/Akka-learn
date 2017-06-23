package com.mjduan.project.example1_basic;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.testkit.javadsl.TestKit;

import org.junit.Before;
import org.junit.Test;

/**
 * Hans  2017-06-23 20:08
 */
public class Test2_lifecycleMonitoring {
    private ActorSystem actorSystem;

    @Before
    public void before() {
        actorSystem = ActorSystem.create("mySystem");
    }

    @Test
    public void test() {
        TestKit testKit = new TestKit(actorSystem);

        ActorRef actorRef = actorSystem.actorOf(WatchActor.props());
        actorRef.tell("kill", testKit.getRef());

        testKit.expectMsg("terminated-msg");
    }


    public static final class WatchActor extends AbstractActor {
        private final ActorRef child = getContext().actorOf(Props.empty(), "target");
        private LoggingAdapter LOG = Logging.getLogger(this);
        private ActorRef lastSender = ActorRef.noSender();

        public WatchActor() {
            getContext().watch(child);
        }

        public static Props props() {
            return Props.create(WatchActor.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .matchEquals("kill", s -> processKill(s))
                    .match(Terminated.class, t -> processTerminated(t))
                    .build();
        }

        private void processTerminated(Terminated t) {
            if (child == t.getActor()) {
                LOG.info("The actor is killed");
            } else {
                LOG.warning("Unexpected");
            }
            lastSender.tell("terminated-msg", self());
        }

        private void processKill(String s) {
            getContext().stop(child);
            lastSender = getSender();
            LOG.info("Rec kill command, so kill child actor");
        }
    }


}
