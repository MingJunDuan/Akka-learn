package com.mjduan.project.example3.src;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.FiniteDuration;

import lombok.Getter;
import lombok.Setter;

import com.mjduan.project.example3.src.manager.RequestTrackDevice;

/**
 * Hans  2017-06-22 01:44
 */
public class DeviceGroup extends AbstractActor {
    final String groupId;
    final Map<String, ActorRef> deviceIdToActor = new HashMap<>();
    final Map<ActorRef, String> actorToDeviceId = new HashMap<>();
    private LoggingAdapter LOG = Logging.getLogger(this);

    public DeviceGroup(String groupId) {
        this.groupId = groupId;
    }

    public static Props props(String groupId) {
        return Props.create(DeviceGroup.class, groupId);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RequestTrackDevice.class, this::processRequestTrackDevice)
                .match(RequestDeviceList.class, this::onDeviceList)
                .match(RequestAllTemperatures.class, this::processRequestAllTemperatures)
                .match(Terminated.class, this::processOnTerminated)
                .build();
    }

    private void processRequestAllTemperatures(RequestAllTemperatures r) {
        getContext().actorOf(DeviceGroupQuery.props(actorToDeviceId, r.getRequestId(), getSender(), new FiniteDuration(3, TimeUnit.SECONDS)));
    }

    private void processRequestTrackDevice(RequestTrackDevice r) {
        if (groupId.equals(r.getGroupId())) {
            ActorRef deviceActor = deviceIdToActor.get(r.getDeviceId());
            if (null != deviceActor) {
                deviceActor.forward(r, getContext());
            } else {
                LOG.info("Creating device actor for deviceId={}", r.getDeviceId());
                deviceActor = getContext().actorOf(DeviceExample3.props(groupId, r.getDeviceId()));
                deviceIdToActor.put(r.getDeviceId(), deviceActor);
                actorToDeviceId.put(deviceActor, r.getDeviceId());
                deviceActor.forward(r, getContext());
            }
        } else {
            LOG.warning("Ignoring request for groupId={}. This actor is groupId={}", r.getGroupId(), groupId);
        }
    }

    private void onDeviceList(RequestDeviceList r) {
        getSender().tell(new ReplyDeviceList(r.requestId, deviceIdToActor.keySet()), self());
    }

    private void processOnTerminated(Terminated t) {
        ActorRef deviceActor = t.getActor();
        String deviceId = getDeviceId(deviceActor);
        LOG.info("Device actor for deviceId={} has been terminated", deviceId);
        deviceIdToActor.remove(deviceId);
        actorToDeviceId.remove(deviceActor);
    }

    private String getDeviceId(ActorRef deviceActor) {
        Set<Map.Entry<ActorRef, String>> entries = actorToDeviceId.entrySet();
        for (Map.Entry<ActorRef, String> entry : entries) {
            if (entry.getKey() == deviceActor) {
                return entry.getValue();
            }
        }

        return null;
    }

    @Getter
    @Setter
    public static final class RequestDeviceList {
        private String requestId;

        public RequestDeviceList(String requestId) {
            this.requestId = requestId;
        }
    }

    @Getter
    @Setter
    public static final class ReplyDeviceList {
        private String requestId;
        private Set<String> deviceIds = Collections.emptySet();

        public ReplyDeviceList(String requestId, Set<String> deviceIds) {
            this.requestId = requestId;
            this.deviceIds = deviceIds;
        }
    }

    @Getter
    @Setter
    public static final class RequestAllTemperatures {
        final long requestId;

        public RequestAllTemperatures(long requestId) {
            this.requestId = requestId;
        }
    }

    @Getter
    @Setter
    public static final class RespondAllTemperatures {
        final long requestId;
        final Map<String, DeviceGroupQuery.TemperatureReading> temperatures;

        public RespondAllTemperatures(long requestId, Map<String, DeviceGroupQuery.TemperatureReading> temperatures) {
            this.requestId = requestId;
            this.temperatures = temperatures;
        }
    }
}
