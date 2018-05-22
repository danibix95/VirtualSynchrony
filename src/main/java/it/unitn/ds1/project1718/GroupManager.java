package it.unitn.ds1.project1718;

import akka.actor.Props;
import it.unitn.ds1.project1718.Messages.*;

import java.time.Duration;

public class GroupManager extends Node {
    private final int MULTICAST_TIMEOUT = 5000;

    public GroupManager() {
        super(0);
    }

    static public Props props() {
        return Props.create(GroupManager.class, GroupManager::new);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(StartMessage.class, this::onStartMessage)
            .match(TimeoutMessage.class, this::onTimeout)
            .build();
    }

    // schedule a Timeout message in specified time
    void setTimeout(int time, int messageId) {
        getContext().system().scheduler().scheduleOnce(
            Duration.ofMillis(time),
            getSelf(),
            new TimeoutMessage(messageId),
            getContext().system().dispatcher(),
            getSelf()
        );
    }

    public void onStartMessage(StartMessage msg) {
        setGroup(msg);
    }

    public void onTimeout(TimeoutMessage tOutMsg) {

    }

}
