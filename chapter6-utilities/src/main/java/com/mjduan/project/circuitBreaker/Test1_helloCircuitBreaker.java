package com.mjduan.project.circuitBreaker;

import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.CircuitBreaker;

import scala.concurrent.duration.Duration;

import org.junit.Before;

/**
 * Hans 2017-06-28 00:56
 */
public class Test1_helloCircuitBreaker {
    private ActorSystem actorSystem;

    @Before
    public void before(){
        actorSystem = ActorSystem.create();
    }


    public static final class DangerousJavaActor extends AbstractActor{
        private final LoggingAdapter LOG = Logging.getLogger(this);
        private final CircuitBreaker breaker;

        public DangerousJavaActor(){
            this.breaker = new CircuitBreaker(getContext().dispatcher(),
                    getContext().system().scheduler(),5,
                    Duration.create(10, TimeUnit.SECONDS),
                    Duration.create(5,TimeUnit.MINUTES)).onOpen(this::notifyMeOnOpen);
        }

        @Override
        public Receive createReceive() {
            return null;
        }

        public void notifyMeOnOpen(){
            LOG.warning("My circuitbreaker is now open");
        }
    }
}
