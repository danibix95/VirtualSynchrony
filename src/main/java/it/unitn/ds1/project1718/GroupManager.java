package it.unitn.ds1.project1718;

import akka.actor.ActorRef;
import akka.actor.Props;
import it.unitn.ds1.project1718.Messages.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.stream.Collectors;

public class GroupManager extends Node {
    private final int MULTICAST_TIMEOUT = 5000;
    private HashMap<ActorRef, Integer> lastMessages = new HashMap<>();

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
            .match(DataMessage.class, this::onDataMessage)
            .match(TimeoutMessage.class, this::onTimeout)
            .match(JoinMessage.class, this::onJoinMessage)
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
        for (ActorRef actor : this.participants) {
            lastMessages.put(actor, -1);
        }
    }

    public void onDataMessage(DataMessage msg) {
        lastMessages.put(getSender(), msg.id);
        setTimeout(MULTICAST_TIMEOUT, msg.id);
    }

    public void onTimeout(TimeoutMessage msg) {
        ActorRef sender = getSender();
        if (lastMessages.get(sender) == msg.checkId) {
            multicast(new ViewChangeMessage(
                participants.stream().filter((node) -> node.equals(sender)).collect(Collectors.toList())
            ));
        }
    }

    public void onJoinMessage(JoinMessage msg) {

    }
}
