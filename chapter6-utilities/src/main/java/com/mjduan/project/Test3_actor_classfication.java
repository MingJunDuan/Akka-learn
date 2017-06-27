package com.mjduan.project;

import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.japi.ManagedActorEventBus;
import akka.testkit.javadsl.TestKit;

import scala.concurrent.duration.FiniteDuration;

import lombok.Getter;
import lombok.Setter;
import org.junit.Before;
import org.junit.Test;

/**
 * Hans 2017-06-27 22:36
 */
public class Test3_actor_classfication {
    private ActorSystem actorSystem;

    @Before
    public void before(){
        actorSystem = ActorSystem.create();
    }

    @Test
    public void test_1(){
        ActorRef observer1 = new TestKit(actorSystem).getRef();
        ActorRef observer2 = new TestKit(actorSystem).getRef();

        TestKit probe1 = new TestKit(actorSystem);
        TestKit probe2 = new TestKit(actorSystem);

        ActorRef subscriber1 = probe1.getRef();
        ActorRef subscriber2 = probe2.getRef();

        ActorBusImpl actorBus = new ActorBusImpl(actorSystem);
        actorBus.subscribe(subscriber1,observer1);
        actorBus.subscribe(subscriber2,observer1);
        actorBus.subscribe(subscriber2,observer2);

        Notification notification = new Notification(observer1, 100);
        actorBus.publish(notification);
        probe1.expectMsgEquals(notification);
        probe2.expectMsgEquals(notification);

        Notification notification1 = new Notification(observer2, 101);
        actorBus.publish(notification1);
        probe1.expectNoMsg(FiniteDuration.create(500, TimeUnit.MILLISECONDS));
        probe2.expectMsgEquals(notification1);


    }
    
    
    @Getter
    @Setter
    public static final class Notification{
        final ActorRef ref;
        final int id;

        public Notification(ActorRef ref, int id) {
            this.ref = ref;
            this.id = id;
        }
    }

    public static final class ActorBusImpl extends ManagedActorEventBus<Notification>{


        public ActorBusImpl(ActorSystem system) {
            super(system);
        }

        @Override
        public int mapSize() {
            return 128;
        }

        @Override
        public ActorRef classify(Notification notification) {
            return notification.ref;
        }
    }
}
