package com.mjduan.project.example1.src;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;

/**
 * Hans  2017-06-21 22:20
 */
public class App extends AbstractActor {
    private LoggingAdapter LOG = Logging.getLogger(this);
    private String name;

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(Request.class, request -> {
                    LOG.info("Rec {}", request);
                    name = request.getName();
                })
                .matchAny(any -> {
                    LOG.error("Rec unknown msg");
                })
                .build();
    }


    public String getName() {
        return name;
    }
}
