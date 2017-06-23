package com.mjduan.project.example3.test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.mjduan.project.example3.src.DeviceExample3;
import com.mjduan.project.example3.src.manager.DeviceRegistered;
import com.mjduan.project.example3.src.manager.RequestTrackDevice;

/**
 * Hans  2017-06-22 01:33
 */
public class DeviceExample3Test {

    private ActorSystem actorSystem;

    @Before
    public void before() {
        actorSystem = ActorSystem.create("deviceActorSystem");
    }

    @Test
    public void test_replyToRegistrationRequest() {
        TestKit testKit = new TestKit(actorSystem);
        ActorRef deviceActor = actorSystem.actorOf(DeviceExample3.props("group", "device"));

        deviceActor.tell(new RequestTrackDevice("group", "device"), testKit.getRef());
        testKit.expectMsgClass(DeviceRegistered.class);
        Assert.assertEquals(deviceActor, testKit.getLastSender());
    }

    @Test
    public void test_ignoreWrongRegistrationRequest() {
        TestKit testKit = new TestKit(actorSystem);
        ActorRef deviceActor = actorSystem.actorOf(DeviceExample3.props("group", "device"));

        deviceActor.tell(new RequestTrackDevice("wrongGroup", "device"), testKit.getRef());
        testKit.expectNoMsg();

        deviceActor.tell(new RequestTrackDevice("group", "wrongDevice"), testKit.getRef());
        testKit.expectNoMsg();
    }

}
