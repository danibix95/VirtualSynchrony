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
    private int lastViewID = 0;
    private int lastAssignedID = 0;
    private final static int MULTICAST_TIMEOUT = 5000;
    private HashMap<ActorRef, Integer> lastMessages = new HashMap<>();
    private View lastGeneratedView;

    public GroupManager() {
        super();
        this.id = 0;
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
            .match(FlushTimeoutMessage.class, this::onFlushTimeout)
            .match(ViewChangeMessage.class, this::onViewChangeMessage)
            .match(FlushMessage.class, this::onFlushMessage)
            .match(JoinMessage.class, this::onJoinMessage)
            .match(A2AMessage.class, this::onA2AMessage)
            .build();
    }

    private void scheduleTimeout(int time, int messageID, ActorRef sender) {
        getContext().system().scheduler().scheduleOnce(
            Duration.ofMillis(time),
            getSelf(),
            new TimeoutMessage(messageID, sender),
            getContext().system().dispatcher(),
            sender
        );
    }

    private void scheduleFlushTimeout(int time, View view, ActorRef sender) {
        getContext().system().scheduler().scheduleOnce(
            Duration.ofMillis(time),
            getSelf(),
            new FlushTimeoutMessage(view, sender),
            getContext().system().dispatcher(),
            sender
        );
    }

    private void onStartMessage(StartMessage msg) {
        currentView = msg.view;
        lastGeneratedView = currentView;
        System.out.format("Group Manager initialized with view %d\n", currentView.id);
    }

    protected void onDataMessage(DataMessage msg) {
        super.onDataMessage(msg);
        lastMessages.put(getSender(), msg.id);
        scheduleTimeout(MULTICAST_TIMEOUT, msg.id, getSender());
    }

    protected void onStableMessage(StableMessage msg) {
        super.onStableMessage(msg);
        lastMessages.put(getSender(), msg.messageID);
        scheduleTimeout(MULTICAST_TIMEOUT, msg.messageID, getSender());
    }

    private void onTimeout(TimeoutMessage msg) {
        // notice: the sender of this message was set as it was sent by the timed out node
        if (lastMessages.getOrDefault(getSender(), -1) == msg.checkID) {
            lastViewID++;

            View updatedView = new View(
                lastViewID,
                lastGeneratedView.members.stream()
                    .filter((node) -> !node.equals(getSender()))
                    .collect(Collectors.toList())
            );

            multicastToView(new ViewChangeMessage(updatedView), updatedView);
            getSelf().tell(new ViewChangeMessage(updatedView), getSelf());
        }
    }

    private void onFlushTimeout(FlushTimeoutMessage msg) {
        if (receivedFlush.containsKey(msg.view)) {
            if (!receivedFlush.get(msg.view).contains(getSender())) {
                View updatedView = new View(
                    lastViewID,
                    lastGeneratedView.members.stream()
                        .filter((node) -> !node.equals(getSender()))
                        .collect(Collectors.toList())
                );

                multicastToView(new ViewChangeMessage(updatedView), updatedView);
                getSelf().tell(new ViewChangeMessage(updatedView), getSelf());
            }
        }
    }

    // check timeout flush
    // if previous view not present => ok
    // otherwise if present check if flush was delivered

    private void onJoinMessage(JoinMessage msg) {
        System.out.format("%s requested to join the system\n", getSender().path().name());
        List<ActorRef> updatedMembers = new ArrayList<>(lastGeneratedView.members);
        updatedMembers.add(getSender());

        // assign the ID to the requesting node
        lastAssignedID++;
        getSender().tell(new AssignIDMessage(lastAssignedID), getSelf());

        lastViewID++;
        View updatedView = new View(lastViewID, updatedMembers);
        lastGeneratedView = updatedView;
        multicastToView(new ViewChangeMessage(updatedView), updatedView);
        getSelf().tell(new ViewChangeMessage(updatedView), getSelf());

        for (ActorRef actor : updatedView.members) {
            if (!actor.equals(getSelf())) {
                scheduleFlushTimeout(MULTICAST_TIMEOUT, updatedView, actor);
            }
        }
    }
}
