package com.mjduan.project.example3.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;

import scala.concurrent.duration.FiniteDuration;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.mjduan.project.example3.src.DeviceExample3;
import com.mjduan.project.example3.src.DeviceGroupQuery;

/**
 * Hans 2017-06-23 09:00
 */
public class DeviceGroupQueryTest {
    private ActorSystem actorSystem;

    @Before
    public void before() {
        actorSystem = ActorSystem.create("mySystem");
    }

    @Test
    public void test_returnTemperatureValueForWorkingDevices() {
        TestKit requester = new TestKit(actorSystem);

        TestKit device1 = new TestKit(actorSystem);
        TestKit device2 = new TestKit(actorSystem);

        Map<ActorRef, String> actorToDevice = new HashMap<>();
        actorToDevice.put(device1.getRef(), "device1");
        actorToDevice.put(device2.getRef(), "device2");

        ActorRef queryGroupActor = actorSystem.actorOf(DeviceGroupQuery
                .props(actorToDevice, 1L, requester.getRef(), new FiniteDuration(3, TimeUnit.SECONDS)));

        Assert.assertEquals(0L, device1.expectMsgClass(DeviceExample3.ReadTemperature.class).getRequestId());
        Assert.assertEquals(0L, device2.expectMsgClass(DeviceExample3.ReadTemperature.class).getRequestId());

        queryGroupActor.tell(new DeviceExample3.RespondTemperature(0L, Optional.of(2.0)), device1.getRef());
        queryGroupActor.tell(new DeviceExample3.RespondTemperature(0L, Optional.of(3.0)), device2.getRef());

        DeviceGroupQuery.RespondAllTemperatures respondAllTemperatures = requester.expectMsgClass(DeviceGroupQuery.RespondAllTemperatures.class);
        Assert.assertEquals(1L, respondAllTemperatures.getRequestId());

        Assert.assertEquals(2, respondAllTemperatures.getTemperatureReadingMap().size());
        Assert.assertEquals(2.0, ((DeviceGroupQuery.Temperature) (respondAllTemperatures.getTemperatureReadingMap().get("device1"))).getValue(), 0);
        Assert.assertEquals(3.0, ((DeviceGroupQuery.Temperature) (respondAllTemperatures.getTemperatureReadingMap().get("device2"))).getValue(), 0);
    }

    @Test
    public void test_returnTemperatureNotAvailableForDevicesWithNoReadings() {
        TestKit requester = new TestKit(actorSystem);

        TestKit device1 = new TestKit(actorSystem);
        TestKit device2 = new TestKit(actorSystem);

        Map<ActorRef, String> actorToDevice = new HashMap<>();
        actorToDevice.put(device1.getRef(), "device1");
        actorToDevice.put(device2.getRef(), "device2");

        ActorRef queryGroupActor = actorSystem.actorOf(DeviceGroupQuery
                .props(actorToDevice, 2L, requester.getRef(), new FiniteDuration(3, TimeUnit.SECONDS)));

        Assert.assertEquals(0L, device1.expectMsgClass(DeviceExample3.ReadTemperature.class).getRequestId());
        Assert.assertEquals(0L, device2.expectMsgClass(DeviceExample3.ReadTemperature.class).getRequestId());

        queryGroupActor.tell(new DeviceExample3.RespondTemperature(0L, Optional.empty()), device1.getRef());
        queryGroupActor.tell(new DeviceExample3.RespondTemperature(0L, Optional.of(3.0)), device2.getRef());

        DeviceGroupQuery.RespondAllTemperatures respondAllTemperatures = requester.expectMsgClass(DeviceGroupQuery.RespondAllTemperatures.class);
        Assert.assertEquals(2L, respondAllTemperatures.getRequestId());

        Assert.assertEquals(2, respondAllTemperatures.getTemperatureReadingMap().size());
        Assert.assertTrue(respondAllTemperatures.getTemperatureReadingMap().get("device1") instanceof DeviceGroupQuery.TemperatureNotAvailable);
        Assert.assertTrue(respondAllTemperatures.getTemperatureReadingMap().get("device2") instanceof DeviceGroupQuery.Temperature);

        DeviceGroupQuery.Temperature temperature = (DeviceGroupQuery.Temperature) respondAllTemperatures.getTemperatureReadingMap().get("device2");
        Assert.assertEquals(3.0, temperature.getValue(), 0);
    }

    @Test
    public void test_returnDeviceNotAvailableIfDeviceStopsBeforeAnswering() {
        TestKit requester = new TestKit(actorSystem);

        TestKit device1 = new TestKit(actorSystem);
        TestKit device2 = new TestKit(actorSystem);

        Map<ActorRef, String> actorToDevice = new HashMap<>();
        actorToDevice.put(device1.getRef(), "device1");
        actorToDevice.put(device2.getRef(), "device2");

        ActorRef queryGroupActor = actorSystem.actorOf(DeviceGroupQuery
                .props(actorToDevice, 2L, requester.getRef(), new FiniteDuration(3, TimeUnit.SECONDS)));

        Assert.assertEquals(0L, device1.expectMsgClass(DeviceExample3.ReadTemperature.class).getRequestId());
        Assert.assertEquals(0L, device2.expectMsgClass(DeviceExample3.ReadTemperature.class).getRequestId());

        queryGroupActor.tell(new DeviceExample3.RespondTemperature(0L, Optional.of(3.0)), device1.getRef());
        device2.getRef().tell(PoisonPill.getInstance(), ActorRef.noSender());

        DeviceGroupQuery.RespondAllTemperatures respondAllTemperatures = requester.expectMsgClass(DeviceGroupQuery.RespondAllTemperatures.class);
        Assert.assertEquals(2L, respondAllTemperatures.getRequestId());

        DeviceGroupQuery.Temperature temperature = (DeviceGroupQuery.Temperature) respondAllTemperatures.getTemperatureReadingMap().get("device1");
        Assert.assertEquals(3.0, temperature.getValue(), 0);

        Assert.assertTrue(respondAllTemperatures.getTemperatureReadingMap().get("device2") instanceof DeviceGroupQuery.DeviceNotAvailable);
    }

    @Test
    public void test_returnTemperatureReadingEvenIfDeviceStopsAfterAnswering() {
        TestKit requester = new TestKit(actorSystem);

        TestKit device1 = new TestKit(actorSystem);
        TestKit device2 = new TestKit(actorSystem);

        Map<ActorRef, String> actorToDevice = new HashMap<>();
        actorToDevice.put(device1.getRef(), "device1");
        actorToDevice.put(device2.getRef(), "device2");

        ActorRef queryGroupActor = actorSystem.actorOf(DeviceGroupQuery
                .props(actorToDevice, 1L, requester.getRef(), new FiniteDuration(3, TimeUnit.SECONDS)));

        Assert.assertEquals(0L, device1.expectMsgClass(DeviceExample3.ReadTemperature.class).getRequestId());
        Assert.assertEquals(0L, device2.expectMsgClass(DeviceExample3.ReadTemperature.class).getRequestId());

        queryGroupActor.tell(new DeviceExample3.RespondTemperature(0L, Optional.of(2.0)), device1.getRef());
        queryGroupActor.tell(new DeviceExample3.RespondTemperature(0L, Optional.of(3.0)), device2.getRef());
        device2.getRef().tell(PoisonPill.getInstance(), ActorRef.noSender());
        device1.getRef().tell(PoisonPill.getInstance(), ActorRef.noSender());

        DeviceGroupQuery.RespondAllTemperatures respondAllTemperatures = requester.expectMsgClass(DeviceGroupQuery.RespondAllTemperatures.class);
        Assert.assertEquals(1L, respondAllTemperatures.getRequestId());

        Assert.assertEquals(2, respondAllTemperatures.getTemperatureReadingMap().size());
        Assert.assertEquals(2.0, ((DeviceGroupQuery.Temperature) (respondAllTemperatures.getTemperatureReadingMap().get("device1"))).getValue(), 0);
        Assert.assertEquals(3.0, ((DeviceGroupQuery.Temperature) (respondAllTemperatures.getTemperatureReadingMap().get("device2"))).getValue(), 0);
    }

    @Test
    public void test_returnDeviceTimedOutIfDeviceDoesNotAnswerInTime() {
        TestKit requester = new TestKit(actorSystem);

        TestKit device1 = new TestKit(actorSystem);
        TestKit device2 = new TestKit(actorSystem);

        Map<ActorRef, String> actorToDevice = new HashMap<>();
        actorToDevice.put(device1.getRef(), "device1");
        actorToDevice.put(device2.getRef(), "device2");

        ActorRef queryGroupActor = actorSystem.actorOf(DeviceGroupQuery
                .props(actorToDevice, 1L, requester.getRef(), new FiniteDuration(3, TimeUnit.SECONDS)));

        Assert.assertEquals(0L, device1.expectMsgClass(DeviceExample3.ReadTemperature.class).getRequestId());
        Assert.assertEquals(0L, device2.expectMsgClass(DeviceExample3.ReadTemperature.class).getRequestId());

        queryGroupActor.tell(new DeviceExample3.RespondTemperature(0L, Optional.of(2.0)), device1.getRef());

        DeviceGroupQuery.RespondAllTemperatures respondAllTemperatures = requester.expectMsgClass(FiniteDuration.create(5, TimeUnit.SECONDS), DeviceGroupQuery.RespondAllTemperatures.class);
        Assert.assertEquals(1L, respondAllTemperatures.getRequestId());

        DeviceGroupQuery.Temperature temperature = (DeviceGroupQuery.Temperature) respondAllTemperatures.getTemperatureReadingMap().get("device1");
        Assert.assertEquals(2.0, temperature.getValue(), 0);

        Assert.assertTrue(respondAllTemperatures.getTemperatureReadingMap().get("device2") instanceof DeviceGroupQuery.DeviceTimeOut);
    }

}
