package com.mjduan.project.example2_typedActor;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import akka.japi.function.Function2;
import akka.typed.ActorRef;
import akka.typed.ActorSystem;
import akka.typed.Behavior;
import akka.typed.javadsl.Actor;
import akka.typed.javadsl.ActorContext;
import akka.typed.javadsl.AskPattern;
import akka.util.Timeout;

import lombok.Getter;
import lombok.Setter;
import org.junit.Before;
import org.junit.Test;

/**
 * Hans  2017-06-24 21:49
 */
public class Test1_helloworld {
    private ActorSystem<Greet> actorSystem;
    private Behavior<Greet> greeter = Actor.immutable((ctx, msg) -> {
        System.out.println("Hello " + msg.getWhom() + "!");
        msg.getReplyTo().tell(new Greeted(msg.getWhom()));
        return Actor.same();
    });

    @Before
    public void before() {
        actorSystem = ActorSystem.create("hello", greeter);
    }

    @Test
    public void test() {
        CompletionStage<Greeted> stage = AskPattern.ask(actorSystem, (ActorRef<Greeted> replyTo) ->
                        new Greet(" world", replyTo),
                new Timeout(3, TimeUnit.SECONDS),
                actorSystem.scheduler());
        stage.thenAccept(greeting -> {
            System.out.println("Result: " + greeting.getWhom());
            actorSystem.terminate();
        });
    }

    @Getter
    @Setter
    public static final class Greet {
        final String whom;
        final ActorRef<Greeted> replyTo;

        public Greet(String whom, ActorRef<Greeted> replyTo) {
            this.whom = whom;
            this.replyTo = replyTo;
        }
    }

    @Getter
    @Setter
    public static final class Greeted {
        final String whom;

        public Greeted(String whom) {
            this.whom = whom;
        }
    }


}
