package com.mjduan.project.example3.src;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import lombok.Getter;
import lombok.Setter;
import scala.concurrent.duration.FiniteDuration;

/**
 * Hans  2017-06-22 20:51
 */
public class DeviceGroupQuery extends AbstractActor {
    final Map<ActorRef, String> actorToDeviceId;
    final long requestId;
    final ActorRef requester;
    private final LoggingAdapter LOG = Logging.getLogger(this);
    Cancellable queryTimeoutTimer;

    public DeviceGroupQuery(Map<ActorRef, String> actorToDeviceId,
                            long requestId, ActorRef requester, FiniteDuration timeout) {
        this.actorToDeviceId = actorToDeviceId;
        this.requestId = requestId;
        this.requester = requester;

        queryTimeoutTimer = getContext().getSystem().scheduler()
                .scheduleOnce(timeout, getSelf(), new CollectionTimeout(), getContext().dispatcher(), getSelf());
    }

    public static Props props(Map<ActorRef, String> actorToDeviceId, long requestId, ActorRef requester, FiniteDuration timeout) {
        return Props.create(DeviceGroupQuery.class, actorToDeviceId, requestId, requester, timeout);
    }

    @Override
    public Receive createReceive() {
        return waitingForReplies(new HashMap<>(), actorToDeviceId.keySet());
    }

    public Receive waitingForReplies(Map<String, TemperatureReading> repliesSoFar, Set<ActorRef> stillWaiting) {
        return receiveBuilder()
                .match(DeviceExample3.RespondTemperature.class, r -> processRespondTemperature(r,repliesSoFar,stillWaiting))
                .match(Terminated.class, t -> processTerminated(t,repliesSoFar,stillWaiting))
                .match(CollectionTimeout.class, c -> processCollectionTimeout(c,repliesSoFar,stillWaiting))
                .build();
    }

    private void processCollectionTimeout(CollectionTimeout c, Map<String, TemperatureReading> repliesSoFar, Set<ActorRef> stillWaiting) {
        Map<String, TemperatureReading> replies = new HashMap<>(repliesSoFar);
        for (ActorRef deviceActor : stillWaiting) {
            String deviceId = actorToDeviceId.get(deviceActor);
            replies.put(deviceId,new DeviceTimeOut());
        }
        requester.tell(new RespondAllTemperatures(requestId,replies),self());
        getContext().stop(getSelf());
    }

    private void processTerminated(Terminated t, Map<String, TemperatureReading> repliesSoFar, Set<ActorRef> stillWaiting) {
        receiveResponse(t.getActor(),new DeviceNotAvailable(),stillWaiting,repliesSoFar);
    }

    private void processRespondTemperature(DeviceExample3.RespondTemperature r, Map<String, TemperatureReading> repliesSoFar, Set<ActorRef> stillWaiting) {
        ActorRef deviceActor = getSender();
        TemperatureReading reading = r.getValue()
                .map(v -> (TemperatureReading) new Temperature(v))
                .orElse(new TemperatureNotAvailable());
        receiveResponse(deviceActor,reading,stillWaiting,repliesSoFar);
    }

    public void receiveResponse(ActorRef deviceActor, TemperatureReading reading, Set<ActorRef> stillWaiting,
                                Map<String, TemperatureReading> repliesSoFar) {
        getContext().unwatch(deviceActor);
        String deviceId = actorToDeviceId.get(deviceActor);
        HashSet<ActorRef> newStillWaiting = new HashSet<>(stillWaiting);
        newStillWaiting.remove(deviceActor);

        HashMap<String, TemperatureReading> newRepliesSoFar = new HashMap<>(repliesSoFar);
        newRepliesSoFar.put(deviceId, reading);
        if (newStillWaiting.isEmpty()) {
            requester.tell(new RespondAllTemperatures(requestId, newRepliesSoFar), getSelf());
            getContext().stop(self());
        } else {
            getContext().become(waitingForReplies(newRepliesSoFar, newStillWaiting));
        }

    }

    @Override
    public void preStart() throws Exception {
        for (ActorRef deviceActor : actorToDeviceId.keySet()) {
            getContext().watch(deviceActor);
            deviceActor.tell(new DeviceExample3.ReadTemperature(0L), getSelf());
        }
    }

    @Override
    public void postStop() throws Exception {
        queryTimeoutTimer.cancel();
    }

    public static interface TemperatureReading {
    }

    public static final class CollectionTimeout {
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
        final Map<String, TemperatureReading> temperatureReadingMap;

        public RespondAllTemperatures(long requestId, Map<String, TemperatureReading> temperatureReadingMap) {
            this.requestId = requestId;
            this.temperatureReadingMap = temperatureReadingMap;
        }
    }

    @Getter
    @Setter
    public static final class Temperature implements TemperatureReading {
        final double value;

        public Temperature(double value) {
            this.value = value;
        }
    }

    public static final class TemperatureNotAvailable implements TemperatureReading {
    }

    public static final class DeviceNotAvailable implements TemperatureReading {
    }

    public static final class DeviceTimeOut implements TemperatureReading {
    }


}

