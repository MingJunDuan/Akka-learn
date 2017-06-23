package com.mjduan.project.example2.src;

import java.util.Optional;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Hans  2017-06-22 00:27
 */
public class Device extends AbstractActor {
    final String groupId;
    final String deviceId;
    private final LoggingAdapter LOG = Logging.getLogger(this);
    Optional<Double> lastTemperatureReading = Optional.empty();

    public Device(String groupId, String deviceId) {
        this.groupId = groupId;
        this.deviceId = deviceId;
    }

    public static Props props(String groupId, String deviceId) {
        return Props.create(Device.class, groupId, deviceId);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                //update record
                .match(RecordTemperature.class, r -> processRecordTemp(r))
                //query the record
                .match(ReadTemperature.class, r -> processReadRequest(r))
                .build();
    }

    private void processRecordTemp(RecordTemperature r) {
        LOG.info("Recorded temperature: requestId={} value={}", r.requestId, r.value);
        lastTemperatureReading = Optional.of(r.value);
        //acknowledgement
        getSender().tell(new TemperatureRecorded(r.requestId), self());
    }

    private void processReadRequest(ReadTemperature r) {
        LOG.info("Rec {}", r);
        //response to the query request
        getSender().tell(new RespondTemperature(r.requestId, lastTemperatureReading), self());
    }

    @Override
    public void preStart() throws Exception {
        LOG.info("Device actor {}-{} startted", groupId, deviceId);
    }

    @Override
    public void postStop() throws Exception {
        LOG.info("Device actor {}-{} stopped", groupId, deviceId);
    }


    @Getter
    @Setter
    @ToString
    public static final class RecordTemperature {
        final long requestId;
        final double value;

        public RecordTemperature(long requestId, double value) {
            this.requestId = requestId;
            this.value = value;
        }
    }

    @Getter
    @Setter
    @ToString
    public static final class TemperatureRecorded {
        final long requestId;

        public TemperatureRecorded(long requestId) {
            this.requestId = requestId;
        }
    }

    public static final class ReadTemperature {
        long requestId;

        public ReadTemperature(long requestId) {
            this.requestId = requestId;
        }

        @Override
        public String toString() {
            return "ReadTemperature{" +
                    "requestId=" + requestId +
                    '}';
        }
    }

    @Getter
    @Setter
    public static final class RespondTemperature {
        long requestId;
        Optional<Double> value;

        public RespondTemperature(long requestId, Optional<Double> value) {
            this.requestId = requestId;
            this.value = value;
        }

        @Override
        public String toString() {
            return "RespondTemperature{" +
                    "requestId=" + requestId +
                    ", value=" + value +
                    '}';
        }
    }
}
