package com.mjduan.project.eventbus;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import lombok.Getter;
import lombok.Setter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Hans 2017-06-28 00:35
 */
public class Test5_eventstream_subscribingSuperclass {
    private ActorSystem actorSystem;

    @Before
    public void before(){
        actorSystem = ActorSystem.create();
    }

    @After
    public void after(){
        actorSystem.terminate();
    }

    @Test
    public void test(){
        ActorRef deadLetterActor = actorSystem.actorOf(DeadLetterActor.props());
        actorSystem.eventStream().subscribe(deadLetterActor,DeadLetter.class);


        ActorRef jazzListener = actorSystem.actorOf(Listener.props());
        ActorRef musicListener = actorSystem.actorOf(Listener.props());

        actorSystem.eventStream().subscribe(jazzListener,Jazz.class);
        actorSystem.eventStream().subscribe(musicListener,AllKindsOfMusic.class);

        //only musicListener will be notified
        actorSystem.eventStream().publish(new Electronic("Parov Stelar"));

        //jazzListener and musicListener will be notified
        actorSystem.eventStream().publish(new Jazz("Jazz artist"));
    }

    public static final class Listener extends AbstractActor {
        private final LoggingAdapter LOG = Logging.getLogger(this);

        public static Props props(){
            return Props.create(Listener.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(Jazz.class,j->processJazz(j))
                    .match(Electronic.class,e->processElectronic(e))
                    .build();
        }

        private void processElectronic(Electronic e) {
            LOG.info("a {} is listening to {}",getSelf().path().name(),e.getArtist());
        }

        private void processJazz(Jazz j) {
            LOG.info("b {} is listening to {}",getSelf().path().name(),j.getArtist());
        }
    }


    interface AllKindsOfMusic{}

    @Setter
    @Getter
    class Jazz implements AllKindsOfMusic{
        final String artist;

        public Jazz(String artist) {
            this.artist = artist;
        }
    }


    @Getter
    @Setter
    class Electronic implements AllKindsOfMusic{
        final String artist;

        public Electronic(String artist) {
            this.artist = artist;
        }
    }

    public static final class DeadLetterActor extends AbstractActor {
        private final LoggingAdapter LOG = Logging.getLogger(this);

        public static Props props(){
            return Props.create(DeadLetterActor.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(DeadLetter.class, msg->{
                        LOG.info("*********DeadLetterActor rec:{}",msg);
                    })
                    .build();
        }
    }
}
