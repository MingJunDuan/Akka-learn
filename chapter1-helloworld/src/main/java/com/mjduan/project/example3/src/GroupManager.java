package com.mjduan.project.example3.src;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import lombok.Getter;
import lombok.Setter;

import com.mjduan.project.example3.src.manager.RequestTrackDevice;

/**
 * Hans  2017-06-22 19:43
 */
public class GroupManager extends AbstractActor {
    private final LoggingAdapter LOG = Logging.getLogger(this);
    private final Map<String, ActorRef> groupIdToActor = new HashMap<>();

    public static Props props() {
        return Props.create(GroupManager.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RequestTrackDevice.class, r -> processRequestTrackDevice(r))
                .match(RequestGroupList.class, r -> processRequestGroupList(r))
                .match(Terminated.class, t -> processTerminated(t))
                .build();
    }

    private void processRequestGroupList(RequestGroupList r) {
        getSender().tell(new GroupList(r.getRequestId(), groupIdToActor.keySet()), self());
    }

    private void processTerminated(Terminated t) {
        ActorRef groupActor = t.getActor();
        Iterator<Map.Entry<String, ActorRef>> entryIterator = groupIdToActor.entrySet().iterator();
        entryIterator.forEachRemaining(entry -> {
            if (entry.getValue() == groupActor) {
                LOG.info("Group actor for groupId={} has been terminated", entry.getKey());
                groupIdToActor.remove(entry.getKey());
            }
        });

    }

    private void processRequestTrackDevice(RequestTrackDevice r) {
        ActorRef groupActor = groupIdToActor.get(r.getGroupId());
        if (null != groupActor) {
            groupActor.forward(r, getContext());
        } else {
            LOG.info("Creating group actor for groupId={}", r.getGroupId());
            groupActor = getContext().actorOf(DeviceGroup.props(r.getGroupId()));
            getContext().watch(groupActor);
            groupActor.forward(r, getContext());
            groupIdToActor.put(r.getGroupId(), groupActor);
        }
    }

    @Getter
    @Setter
    public static final class RequestGroupList {
        final long requestId;

        public RequestGroupList(long requestId) {
            this.requestId = requestId;
        }
    }

    @Getter
    @Setter
    public static final class GroupList {
        final long requestId;
        final Set<String> groupIds;

        public GroupList(long requestId, Set<String> groupIds) {
            this.requestId = requestId;
            this.groupIds = groupIds;
        }
    }


}
