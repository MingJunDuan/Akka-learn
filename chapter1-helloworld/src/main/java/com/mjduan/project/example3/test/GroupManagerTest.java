package com.mjduan.project.example3.test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.mjduan.project.example3.src.GroupManager;
import com.mjduan.project.example3.src.manager.DeviceRegistered;
import com.mjduan.project.example3.src.manager.RequestTrackDevice;

/**
 * Hans  2017-06-22 20:06
 */
public class GroupManagerTest {
    private ActorSystem actorSystem;
    private TestKit testKit;

    @Before
    public void before() {
        actorSystem = ActorSystem.create("mySystem");
        testKit = new TestKit(actorSystem);
    }

    @Test
    public void test() {
        ActorRef managerActor = actorSystem.actorOf(GroupManager.props());

        managerActor.tell(new RequestTrackDevice("group1", "device"), testKit.getRef());
        testKit.expectMsgClass(DeviceRegistered.class);
        ActorRef deviceActor = testKit.getLastSender();
        deviceActor.tell(PoisonPill.getInstance(), ActorRef.noSender());

        managerActor.tell(new RequestTrackDevice("group2", "device"), testKit.getRef());
        testKit.expectMsgClass(DeviceRegistered.class);

        managerActor.tell(new GroupManager.RequestGroupList(0L), testKit.getRef());
        GroupManager.GroupList groupList = testKit.expectMsgClass(GroupManager.GroupList.class);
        Assert.assertEquals(0L, groupList.getRequestId());
        Assert.assertEquals(Stream.of("group1", "group2").collect(Collectors.toSet()), groupList.getGroupIds());
    }

}
