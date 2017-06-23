package com.mjduan.project.example2.test;

import java.util.Optional;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.mjduan.project.example2.src.Device;

/**
 * Hans  2017-06-22 00:37
 */
public class DeviceTest {
    private ActorSystem actorSystem;

    @Before
    public void before() {
        actorSystem = ActorSystem.create("deviceActorSystem");
    }

    @Test
    public void test() {
        TestKit testKit = new TestKit(actorSystem);
        ActorRef deviceActorRef = actorSystem.actorOf(Device.props("group", "device"));

        deviceActorRef.tell(new Device.RecordTemperature(1L, 24.0), testKit.getRef());
        Assert.assertEquals(1L, testKit.expectMsgClass(Device.TemperatureRecorded.class).getRequestId());

        deviceActorRef.tell(new Device.ReadTemperature(100L), testKit.getRef());
        Device.RespondTemperature response = testKit.expectMsgClass(Device.RespondTemperature.class);
        Assert.assertEquals(100L, response.getRequestId());
        Assert.assertEquals(Optional.of(24.0), response.getValue());


        deviceActorRef.tell(new Device.RecordTemperature(3L, 55.0), testKit.getRef());
        Assert.assertEquals(3L, testKit.expectMsgClass(Device.TemperatureRecorded.class).getRequestId());

        deviceActorRef.tell(new Device.ReadTemperature(101L), testKit.getRef());
        Device.RespondTemperature response2 = testKit.expectMsgClass(Device.RespondTemperature.class);
        Assert.assertEquals(101L, response2.getRequestId());
        Assert.assertEquals(Optional.of(55.0), response2.getValue());
    }

}
