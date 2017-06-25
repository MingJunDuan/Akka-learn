package com.mjduan.project.example1_actorBasic;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.testkit.javadsl.TestKit;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Hans  2017-06-24 02:16
 */
public class Test5_stash {
    private ActorSystem actorSystem;

    @Before
    public void before() {
        actorSystem = ActorSystem.create("mySystem");
    }

    @Test
    public void test() {
        TestKit testKit = new TestKit(actorSystem);
        ActorRef actorRef = actorSystem.actorOf(ActorWithProtocol.props());

        //send a write message
        actorRef.tell(new ActorWithProtocol.Write(1L), testKit.getRef());
        actorRef.tell(new ActorWithProtocol.Write(2L), testKit.getRef());

        actorRef.tell("open", ActorRef.noSender());
        Assert.assertEquals(1L,testKit.expectMsgClass(ActorWithProtocol.ReplyWrite.class).getRequestId());
        Assert.assertEquals(2L,testKit.expectMsgClass(ActorWithProtocol.ReplyWrite.class).getRequestId());

        actorRef.tell(new ActorWithProtocol.Close(100L), testKit.getRef());
        Assert.assertEquals(100L,testKit.expectMsgClass(ActorWithProtocol.ReplyClose.class).getRequestId());

        //then send write message again
        actorRef.tell(new ActorWithProtocol.Write(3L), testKit.getRef());
        actorRef.tell(new ActorWithProtocol.Write(4L), testKit.getRef());

        actorRef.tell("open", ActorRef.noSender());
        Assert.assertEquals(3L,testKit.expectMsgClass(ActorWithProtocol.ReplyWrite.class).getRequestId());
        Assert.assertEquals(4L,testKit.expectMsgClass(ActorWithProtocol.ReplyWrite.class).getRequestId());
    }

    public static final class ActorWithProtocol extends AbstractActorWithStash {
        private final LoggingAdapter LOG = Logging.getLogger(this);

        public static Props props() {
            return Props.create(ActorWithProtocol.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .matchEquals("open", s -> {
                        //enqueue message from actor's stash to mailbox
                        unstashAll();
                        getContext().become(receiveBuilder()
                                .match(Write.class, w -> {
                                    LOG.info("Rec write message, requestId={}", w.getRequestId());
                                    getSender().tell(new ReplyWrite(w.getRequestId()), self());
                                })
                                .match(Close.class, c -> {
                                    LOG.info("Rec close message, closeId={}", c.getRequestId());
                                    getSender().tell(new ReplyClose(c.getRequestId()), self());
                                    getContext().unbecome();
                                })
                                .build(), false);
                    })
                    .matchAny(any -> {
                        LOG.warning("Rec unknown msg {}", any);
                        //add message actor received to actor's stash.
                        stash();
                    })
                    .build();
        }

        public static interface Command {
        }

        @Getter
        @Setter
        @ToString
        public static final class Close implements Command {
            final long requestId;

            public Close(long requestId) {
                this.requestId = requestId;
            }
        }

        @Getter
        @Setter
        public static final class ReplyClose {
            final long requestId;

            public ReplyClose(long requestId) {
                this.requestId = requestId;
            }
        }

        @Setter
        @Getter
        @ToString
        public static final class Write implements Command {
            final long requestId;

            public Write(long requestId) {
                this.requestId = requestId;
            }
        }

        @Getter
        @Setter
        public static final class ReplyWrite {
            final long requestId;

            public ReplyWrite(long requestId) {
                this.requestId = requestId;
            }
        }
    }

}
