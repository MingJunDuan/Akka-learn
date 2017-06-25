package com.mjduan.project.example4_routing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.ActorRefRoutee;
import akka.routing.RandomRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;

import lombok.Getter;
import lombok.Setter;
import org.junit.Before;
import org.junit.Test;

/**
 * Hans  2017-06-25 15:58
 */
public class Test1_simpleDemo {
    private ActorSystem actorSystem;

    @Before
    public void before() {
        actorSystem = ActorSystem.create("mySystem");
    }

    @Test
    public void test() {
        ActorRef masterActor = actorSystem.actorOf(MasterActor.props());
        masterActor.tell(new Work(0L, "payload0"), ActorRef.noSender());
        masterActor.tell(new Work(1L, "payload1"), ActorRef.noSender());

        masterActor.tell(new Work(-1L, "payload-1"), ActorRef.noSender());

        masterActor.tell(new Work(3L, "payload3"), ActorRef.noSender());
        masterActor.tell(new Work(4L, "payload4"), ActorRef.noSender());
    }


    @Getter
    @Setter
    public static final class Work implements Serializable {
        private long requestId;
        private String payload;

        public Work(long requestId, String payload) {
            this.requestId = requestId;
            this.payload = payload;
        }
    }

    public static class WorkerActor extends AbstractActor {
        private LoggingAdapter LOG = Logging.getLogger(this);

        public static Props props() {
            return Props.create(WorkerActor.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(Work.class, w -> processWork(w))
                    .build();
        }

        private void processWork(Work w) {
            LOG.info("Worker rec work with requestId={} and payload={}", w.getRequestId(), w.getPayload());
            long requestId = w.getRequestId();
            if (requestId == -1) {
                getContext().stop(self());
            }
        }
    }

    public static class MasterActor extends AbstractActor {
        private LoggingAdapter LOG = Logging.getLogger(this);
        private Router router;

        {
            List<Routee> routees = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                ActorRef actorRef = getContext().actorOf(WorkerActor.props());
                getContext().watch(actorRef);
                routees.add(new ActorRefRoutee(actorRef));
            }
            router = new Router(new RandomRoutingLogic(), routees);
        }

        public static Props props() {
            return Props.create(MasterActor.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(Work.class, work -> {
                        router.route(work, getSender());
                    })
                    .match(Terminated.class, t -> processTerminated(t))
                    .build();
        }

        private void processTerminated(Terminated t) {
            LOG.info("MasterActor rec a terminated actor");
            router = router.removeRoutee(t.actor());
            ActorRef workerActor = getContext().actorOf(WorkerActor.props());
            getContext().watch(workerActor);
            router = router.addRoutee(new ActorRefRoutee(workerActor));
        }
    }
}

