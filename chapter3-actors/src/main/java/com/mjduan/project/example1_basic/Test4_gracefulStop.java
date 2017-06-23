package com.mjduan.project.example1_basic;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.testkit.javadsl.TestKit;

import org.junit.Before;
import org.junit.Test;

/**
 * Hans  2017-06-24 01:33
 */
public class Test4_gracefulStop {
    private ActorSystem actorSystem;

    @Before
    public void before() {
        actorSystem = ActorSystem.create("mySystem");
    }

    @Test
    public void test() {
        TestKit testKit = new TestKit(actorSystem);
        ActorRef managerActor = actorSystem.actorOf(Manager.props());

        managerActor.tell("job", testKit.getRef());
        testKit.expectMsg("Done");

        //shutdown worker
        managerActor.tell(Manager.ShutDown.SHUT_DOWN, testKit.getRef());
        managerActor.tell("mission", testKit.getRef());
        testKit.expectMsg("Service unavailable");
    }

    public static final class Manager extends AbstractActor {
        private final LoggingAdapter LOG = Logging.getLogger(this);
        private ActorRef worker;

        public static Props props() {
            return Props.create(Manager.class);
        }

        @Override
        public void preStart() throws Exception {
            worker = getContext().actorOf(Worker.props());
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .matchEquals("job", job -> worker.forward(job, getContext()))
                    .matchEquals(ShutDown.SHUT_DOWN, s -> {
                        LOG.info("Manager receive shutdown message");
                        worker.tell(PoisonPill.getInstance(), getSelf());
                        getContext().become(shuttingDown());
                    })
                    .build();
        }

        private Receive shuttingDown() {
            return receiveBuilder()
                    .matchEquals("mission", mission -> {
                        LOG.info("receive mission msg");
                        getSender().tell("Service unavailable", self());
                    })
                    .match(Terminated.class, t -> t.actor().equals(worker), t -> getContext().stop(self()))
                    .build();
        }

        private static enum ShutDown {
            SHUT_DOWN;
        }
    }

    public static final class Worker extends AbstractActor {
        private final LoggingAdapter LOG = Logging.getLogger(this);

        public static Props props() {
            return Props.create(Worker.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .matchEquals("job", work -> processWork(work))
                    .matchAny(any -> LOG.warning("Rec unknown msg {}", any))
                    .build();
        }

        private void processWork(String work) {
            LOG.info("Worker receive a mission....Finished!");
            getSender().tell("Done", getSelf());
        }
    }

}
