package com.mjduan.project.example3_supervision;

import javax.naming.ServiceUnavailableException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.ReceiveTimeout$;
import akka.actor.SupervisorStrategy;
import akka.actor.Terminated;
import akka.dispatch.Mapper;
import akka.event.LoggingReceive;
import akka.japi.Util;
import akka.japi.pf.DeciderBuilder;
import akka.pattern.Patterns;
import akka.util.Timeout;

import scala.concurrent.duration.Duration;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;
import lombok.Setter;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;
import static com.mjduan.project.example3_supervision.Test2_faultHandlingSample.CountServiceApi.GetCurrentCount;
import static com.mjduan.project.example3_supervision.Test2_faultHandlingSample.WorkerApi.Start;

/**
 * Hans  2017-06-24 23:37
 */
public class Test2_faultHandlingSample {

    public static void main(String[] args) {
        Config config = ConfigFactory.parseString(
                "akka.loglevel=\"DEBUG\"\n" +
                        "akka.actor.debug {\n" +
                        "  receive = on\n" +
                        "  lifecycle = on\n" +
                        "}\n");
        ActorSystem system = ActorSystem.create("faultHandlingSystem", config);

        ActorRef workerActor = system.actorOf(Worker.props(), "worker");
        ActorRef listenerActor = system.actorOf(Listener.props(), "listener");

        workerActor.tell(Start, listenerActor);
    }

    public interface WorkerApi {
        public static final Object Start = "Start";
        public static final Object Do = "Do";

        public static class Process {
            public final double percent;

            public Process(double percent) {
                this.percent = percent;
            }

            @Override
            public String toString() {
                return String.format("%s(%s)", getClass().getSimpleName(), percent);
            }
        }
    }

    public interface CountServiceApi {
        public static final Object GetCurrentCount = "GetCurrentCount";

        @Setter
        @Getter
        public static class CurrentCount {
            final String key;
            final long count;

            public CurrentCount(String key, long count) {
                this.key = key;
                this.count = count;
            }

            @Override
            public String toString() {
                return String.format("%s(%s, %s)", getClass().getSimpleName(), key, count);
            }
        }

        @Getter
        @Setter
        public static class Increment {
            final long n;

            public Increment(long n) {
                this.n = n;
            }

            @Override
            public String toString() {
                return String.format("%s(%s)", getClass().getSimpleName(), n);
            }
        }

        public static class ServiceUnavailable extends RuntimeException {
            public ServiceUnavailable(String msg) {
                super(msg);
            }
        }


    }

    public interface CounterApi {
        public static class UseStorage {
            public final ActorRef storage;

            public UseStorage(ActorRef storage) {
                this.storage = storage;
            }

            @Override
            public String toString() {
                return String.format("%s(%s)", getClass().getSimpleName(), storage);
            }
        }
    }

    public interface StorageApi {

        public static class Store {
            public final Entry entry;

            public Store(Entry entry) {
                this.entry = entry;
            }
        }


        public static class Entry {
            public final String key;
            public final long value;

            public Entry(String key, long value) {
                this.key = key;
                this.value = value;
            }

            @Override
            public String toString() {
                return String.format("%s(%s,%s)", getClass().getSimpleName(), key, value);
            }
        }

        public static class Get {
            public final String key;

            public Get(String key) {
                this.key = key;
            }

            @Override
            public String toString() {
                return String.format("%s(%s)", getClass().getSimpleName(), key);
            }
        }

        public static class StorageException extends RuntimeException {
            public StorageException(String msg) {
                super(msg);
            }
        }
    }

    public static final class Listener extends AbstractLoggingActor {

        public static Props props() {
            return Props.create(Listener.class);
        }

        @Override
        public void preStart() throws Exception {
            getContext().setReceiveTimeout(Duration.create(15, TimeUnit.SECONDS));
        }

        @Override
        public Receive createReceive() {
            return LoggingReceive.create(receiveBuilder()
                    .match(WorkerApi.Process.class, p -> process(p))
                    .matchEquals(ReceiveTimeout.getInstance(), x -> processReceiveTimeout(x))
                    .build(), getContext());
        }

        private void processReceiveTimeout(ReceiveTimeout$ x) {
            log().error("Shutting down due to unavailable service");
            getContext().getSystem().terminate();
        }

        private void process(WorkerApi.Process p) {
            log().info("Current progress:{} %", p.percent);
            if (p.percent >= 100.0) {
                log().info("That's all, shutting down");
                getContext().getSystem().terminate();
            }
        }
    }

    public static final class Worker extends AbstractLoggingActor {
        private static final SupervisorStrategy strategy = new OneForOneStrategy(DeciderBuilder
                .match(CountServiceApi.ServiceUnavailable.class, e -> SupervisorStrategy.stop())
                .matchAny(any -> SupervisorStrategy.escalate())
                .build());
        final Timeout askTimeout = new Timeout(Duration.create(5, TimeUnit.SECONDS));
        final ActorRef countService = getContext().actorOf(CountService.props(), "counter");
        final int totalCount = 51;
        private ActorRef processListener;

        public static Props props() {
            return Props.create(Worker.class);
        }

        @Override
        public SupervisorStrategy supervisorStrategy() {
            return strategy;
        }

        @Override
        public Receive createReceive() {
            return LoggingReceive.create(receiveBuilder()
                    .matchEquals(Start, x -> processListener == null, w -> process(w))
                    .matchEquals(WorkerApi.Do, x -> processDo(x))
                    .build(), getContext());
        }

        private void processDo(Object x) {
            countService.tell(new CountServiceApi.Increment(1), self());
            countService.tell(new CountServiceApi.Increment(1), self());
            countService.tell(new CountServiceApi.Increment(1), self());

            Patterns.pipe(
                    Patterns.ask(countService, GetCurrentCount, askTimeout)
                            .mapTo(Util.classTag(CountServiceApi.CurrentCount.class))
                            .map(new Mapper<CountServiceApi.CurrentCount, WorkerApi.Process>() {
                                @Override
                                public WorkerApi.Process apply(CountServiceApi.CurrentCount parameter) {
                                    return super.apply(parameter);
                                }
                            }, getContext().dispatcher()), getContext().dispatcher())
                    .to(processListener);
        }

        private void process(Object w) {
            processListener = getSender();
            getContext().getSystem().scheduler().schedule(
                    Duration.Zero(), Duration.create(1, TimeUnit.SECONDS), getSelf(), WorkerApi.Do, getContext().dispatcher(), null);
        }
    }

    public static class CountService extends AbstractLoggingActor {
        static final Object Reconnect = "Reconnect";
        private static final SupervisorStrategy strategy = new OneForOneStrategy(3, Duration.create(5, TimeUnit.SECONDS), DeciderBuilder
                .match(StorageApi.StorageException.class, e -> restart())
                .matchAny(any -> escalate()).build());
        final String key = self().path().name();
        final List<SenderMsgPair> backlog = new ArrayList<>();
        final int MAX_BACKLOG = 10_000;
        ActorRef storage;
        ActorRef counter;

        public static Props props() {
            return Props.create(CountService.class);
        }

        @Override
        public SupervisorStrategy supervisorStrategy() {
            return strategy;
        }

        @Override
        public void preStart() throws Exception {
            initStorage();
        }

        private void initStorage() {
            storage = getContext().watch(getContext().actorOf(Storage.props()));
            if (counter != null) {
                counter.tell(new CounterApi.UseStorage(storage), self());
            }
            storage.tell(new StorageApi.Get(key), self());
        }

        @Override
        public Receive createReceive() {
            return LoggingReceive.create(receiveBuilder()
                    .match(StorageApi.Entry.class, this::processEntry)
                    .match(CountServiceApi.Increment.class, this::processIncrement)
                    .match(Terminated.class, this::processTerminated)
                    .matchEquals(Reconnect, r -> initStorage())
                    .build(), getContext());
        }

        private void processTerminated(Terminated terminated) {
            storage = null;
            counter.tell(new CounterApi.UseStorage(null), self());
            getContext().getSystem().scheduler().scheduleOnce(Duration.create(10, TimeUnit.SECONDS),
                    self(), Reconnect, getContext().dispatcher(), null);
        }

        private void processIncrement(CountServiceApi.Increment increment) throws ServiceUnavailableException {
            forwardOrPlaceInBacklog(increment);
        }

        void forwardOrPlaceInBacklog(Object msg) throws ServiceUnavailableException {
            if (counter == null) {
                if (backlog.size() >= MAX_BACKLOG) {
                    throw new ServiceUnavailableException("Counter service unavailable, lack of initial value");
                }
                backlog.add(new SenderMsgPair(getSender(), msg));
            } else {
                counter.forward(msg, getContext());
            }
        }

        private void processEntry(StorageApi.Entry entry) {
            long value = entry.value;
            counter = getContext().actorOf(Counter.props(key, value));
            counter.tell(new CounterApi.UseStorage(storage), self());
            for (SenderMsgPair each : backlog) {
                counter.tell(each.msg, each.sender);
            }
            backlog.clear();
        }


        private static class SenderMsgPair {
            final ActorRef sender;
            final Object msg;

            public SenderMsgPair(ActorRef sender, Object msg) {
                this.sender = sender;
                this.msg = msg;
            }
        }
    }

    public static final class Counter extends AbstractLoggingActor {
        final String key;
        long count;
        ActorRef storage;

        public Counter(String key, long count) {
            this.key = key;
            this.count = count;
        }

        public static Props props(String key, long count) {
            return Props.create(Counter.class, key, count);
        }

        @Override
        public Receive createReceive() {
            return LoggingReceive.create(receiveBuilder()
                    .match(CounterApi.UseStorage.class, useStorage -> {
                        storage = useStorage.storage;
                        storeCount();
                    })
                    .match(CountServiceApi.Increment.class, increment -> {
                        count += increment.n;
                        storeCount();
                    })
                    .matchEquals(GetCurrentCount, gcc -> {
                        getSender().tell(new CountServiceApi.CurrentCount(key, count), self());
                    })
                    .build(), getContext());
        }

        void storeCount() {
            if (storage != null) {
                storage.tell(new StorageApi.Store(new StorageApi.Entry(key, count)), getSelf());
            }
        }
    }

    public static final class Storage extends AbstractLoggingActor {
        final DummyDB db = DummyDB.instance;

        public static Props props() {
            return Props.create(Storage.class);
        }

        @Override
        public Receive createReceive() {
            return LoggingReceive.create(receiveBuilder()
                    .match(StorageApi.Store.class, store -> {
                        db.save(store.entry.key, store.entry.value);
                    })
                    .match(StorageApi.Get.class, get -> {
                        Long value = db.load(get.key);
                        getSender().tell(new StorageApi.Entry(get.key, value == null ? Long.valueOf(0L) : value), self());
                    })
                    .build(), getContext());
        }
    }

    public static class DummyDB {
        public static final DummyDB instance = new DummyDB();
        private final Map<String, Long> db = new HashMap<>();

        private DummyDB() {
        }

        public synchronized void save(String key, Long value) {
            if (11 <= value && value <= 14) {
                throw new StorageApi.StorageException("Simulated store failure " + value);
            }
            db.put(key, value);
        }

        public synchronized Long load(String key) {
            return db.get(key);
        }

    }

}
