package it.unitn.ds1.project1718;

import akka.actor.ActorRef;
import akka.actor.Props;
import it.unitn.ds1.project1718.Messages.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class GroupManager extends Node {
    private final static int MULTICAST_TIMEOUT = 5000;
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
            .match(StableMessage.class, this::onStableMessage)
            .match(TimeoutMessage.class, this::onTimeout)
            .match(JoinMessage.class, this::onJoinMessage)
            .build();
    }

    private void scheduleTimeout(int time, int messageId) {
        getContext().system().scheduler().scheduleOnce(
            Duration.ofMillis(time),
            getSelf(),
            new TimeoutMessage(messageId),
            getContext().system().dispatcher(),
            getSelf()
        );
    }

    private void onStartMessage(StartMessage msg) {
        currentView = msg.view;
    }

    protected void onDataMessage(DataMessage msg) {
        super.onDataMessage(msg);
        lastMessages.put(getSender(), msg.id);
        scheduleTimeout(MULTICAST_TIMEOUT, msg.id);
    }

    protected void onStableMessage(StableMessage msg) {
        super.onStableMessage(msg);
        lastMessages.put(getSender(), msg.messageID);
        scheduleTimeout(MULTICAST_TIMEOUT, msg.messageID);
    }

    private void onTimeout(TimeoutMessage msg) {
        ActorRef sender = getSender();
        if (lastMessages.getOrDefault(sender, -1) == msg.checkId) {
            View updatedView = new View(
                currentView.id + 1,
                currentView.members.stream()
                    .filter((node) -> node.equals(sender))
                    .collect(Collectors.toList())
            );

            multicastToView(new ViewChangeMessage(updatedView), updatedView);
        }
    }

    private void onJoinMessage(JoinMessage msg) {
        List<ActorRef> updatedMembers = new ArrayList<>(currentView.members);
        updatedMembers.add(getSender());

        View updatedView = new View(currentView.id + 1, updatedMembers);
        multicastToView(new ViewChangeMessage(updatedView), updatedView);
    }
}
