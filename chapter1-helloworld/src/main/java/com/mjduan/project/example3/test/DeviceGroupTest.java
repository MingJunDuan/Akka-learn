package com.mjduan.project.example3.test;

import java.util.Map;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.mjduan.project.example3.src.DeviceExample3;
import com.mjduan.project.example3.src.DeviceGroup;
import com.mjduan.project.example3.src.DeviceGroupQuery;
import com.mjduan.project.example3.src.manager.DeviceRegistered;
import com.mjduan.project.example3.src.manager.RequestTrackDevice;

/**
 * Hans  2017-06-22 02:13
 */
public class DeviceGroupTest {
    private ActorSystem actorSystem;

    @Before
    public void before() {
        actorSystem = ActorSystem.create("deviceGroupSystem");
    }

    @Test
    public void test_registerDeviceActor() {
        TestKit testKit = new TestKit(actorSystem);
        ActorRef groupActor = actorSystem.actorOf(DeviceGroup.props("group"));

        groupActor.tell(new RequestTrackDevice("group", "device1"), testKit.getRef());
        testKit.expectMsgClass(DeviceRegistered.class);
        ActorRef deviceActor1 = testKit.getLastSender();

        groupActor.tell(new RequestTrackDevice("group", "device2"), testKit.getRef());
        testKit.expectMsgClass(DeviceRegistered.class);
        ActorRef deviceActor2 = testKit.getLastSender();
        Assert.assertNotEquals(deviceActor1, deviceActor2);

        deviceActor1.tell(new DeviceExample3.RecordTemperature(0L, 1.0), testKit.getRef());
        Assert.assertEquals(0L, testKit.expectMsgClass(DeviceExample3.TemperatureRecorded.class).getRequestId());

        deviceActor2.tell(new DeviceExample3.RecordTemperature(1L, 2.0), testKit.getRef());
        Assert.assertEquals(1L, testKit.expectMsgClass(DeviceExample3.TemperatureRecorded.class).getRequestId());
    }

    @Test
    public void test_collectTemperaturesFromAllActiveDevices() {
        TestKit testKit = new TestKit(actorSystem);
        ActorRef groupActor = actorSystem.actorOf(DeviceGroup.props("group1"));

        groupActor.tell(new RequestTrackDevice("group1", "device1"), testKit.getRef());
        testKit.expectMsgClass(DeviceRegistered.class);
        ActorRef deviceActor1 = testKit.getLastSender();

        groupActor.tell(new RequestTrackDevice("group1", "device2"), testKit.getRef());
        testKit.expectMsgClass(DeviceRegistered.class);
        ActorRef deviceActor2 = testKit.getLastSender();

        groupActor.tell(new RequestTrackDevice("group1", "device3"), testKit.getRef());
        testKit.expectMsgClass(DeviceRegistered.class);
        // No temperature for device 3

        deviceActor1.tell(new DeviceExample3.RecordTemperature(0L, 1.0), testKit.getRef());
        Assert.assertEquals(0L, testKit.expectMsgClass(DeviceExample3.TemperatureRecorded.class).getRequestId());
        deviceActor2.tell(new DeviceExample3.RecordTemperature(1L, 2.0), testKit.getRef());
        Assert.assertEquals(1L, testKit.expectMsgClass(DeviceExample3.TemperatureRecorded.class).getRequestId());

        groupActor.tell(new DeviceGroup.RequestAllTemperatures(100L), testKit.getRef());
        DeviceGroupQuery.RespondAllTemperatures response = testKit.expectMsgClass(DeviceGroupQuery.RespondAllTemperatures.class);
        Assert.assertEquals(100L, response.getRequestId());

        Map<String, DeviceGroupQuery.TemperatureReading> readingMap = response.getTemperatureReadingMap();
        Assert.assertEquals(3, readingMap.size());

        assertDevice1Temperature((DeviceGroupQuery.Temperature) readingMap.get("device1"));
        assertDevice2Temperature((DeviceGroupQuery.Temperature) readingMap.get("device2"));
        assertDevice3(readingMap.get("device3"));
    }

    private void assertDevice1Temperature(DeviceGroupQuery.Temperature temperature) {
        Assert.assertEquals(1.0, temperature.getValue(), 0);
    }

    private void assertDevice2Temperature(DeviceGroupQuery.Temperature temperature) {
        Assert.assertEquals(2.0, temperature.getValue(), 0);
    }

    private void assertDevice3(DeviceGroupQuery.TemperatureReading temperatureReading) {
        Assert.assertTrue(temperatureReading instanceof DeviceGroupQuery.TemperatureNotAvailable);
    }

}
