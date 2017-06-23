package com.mjduan.project.example1.test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.TestActorRef;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.mjduan.project.example1.src.App;
import com.mjduan.project.example1.src.Request;

/**
 * Hans  2017-06-21 23:08
 */
public class AppTest {
    private ActorSystem actorSystem;

    @Before
    public void before() {
        actorSystem = ActorSystem.create("myActorSystem");
    }

    @Test
    public void test() {
        TestActorRef<App> actorRef = TestActorRef.create(actorSystem, Props.create(App.class));
        actorRef.tell(Request.builder().name("mjduan").build(), ActorRef.noSender());

        String name = actorRef.underlyingActor().getName();
        Assert.assertEquals("mjduan", name);

    }

}
