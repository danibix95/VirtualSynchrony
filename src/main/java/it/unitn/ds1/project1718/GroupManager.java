package it.unitn.ds1.project1718;

import akka.actor.ActorRef;
import akka.actor.Props;
import it.unitn.ds1.project1718.Messages.*;

import java.time.Duration;
import java.util.ArrayList;
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
            .match(DataMessage.class, this::onDataMessage)
            .match(TimeoutMessage.class, this::onTimeout)
            .match(JoinMessage.class, this::onJoinMessage)
            .build();
    }

    void scheduleTimeout(int time, int messageId) {
        getContext().system().scheduler().scheduleOnce(
            Duration.ofMillis(time),
            getSelf(),
            new TimeoutMessage(messageId),
            getContext().system().dispatcher(),
            getSelf()
        );
    }
//
//    public void onStartMessage(StartMessage msg) {
//        setGroup(msg);
//        for (ActorRef actor : this.participants) {
//            lastMessages.put(actor, -1);
//        }
//    }

    @Override
    public void onDataMessage(DataMessage msg) {
        lastMessages.put(getSender(), msg.id);
        scheduleTimeout(MULTICAST_TIMEOUT, msg.id);
    }

    public void onTimeout(TimeoutMessage msg) {
        ActorRef sender = getSender();
        if (lastMessages.get(sender) == msg.checkId) {
            multicast(new ViewChangeMessage(
                participants.stream().filter((node) -> node.equals(sender))
                            .collect(Collectors.toList())
            ));// use View class instead of the list of participants
        }
    }

    public void onJoinMessage(JoinMessage msg) {
        lastMessages.put(getSender(), -1);
        ArrayList<ActorRef> newView = new ArrayList<ActorRef>(participants);
        newView.add(getSender());
        multicastToView(new ViewChangeMessage(newView), newView);
    }
}
