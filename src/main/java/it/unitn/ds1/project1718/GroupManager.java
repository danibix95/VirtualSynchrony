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
    private final int TIMEOUT = 5500;
    private HashMap<ActorRef, Integer> lastBeat = new HashMap<>();
    private HashMap<ActorRef, Boolean> ignoreTimeout = new HashMap<>();
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
            .match(A2AMessage.class, this::onA2AMessage)        // binding of A2A MUST be before DataMessage
            .match(DataMessage.class, this::onDataMessage)      // hierarchical overshadowing
            .match(StableMessage.class, this::onStableMessage)
            .match(JoinMessage.class, this::onJoinMessage)
            .match(ViewChangeMessage.class, this::onViewChangeMessage)
            .match(TimeoutMessage.class, this::onTimeout)
            .match(FlushTimeoutMessage.class, this::onFlushTimeout)
            .match(FlushMessage.class, this::onFlushMessage)
            .match(HeartbeatMessage.class, this::onHeartbeatMessage)
            .build();
    }

    private void scheduleTimeout(int lastBeatID, ActorRef sender) {
        getContext().system().scheduler().scheduleOnce(
            Duration.ofMillis(TIMEOUT),
            getSelf(),
            new TimeoutMessage(lastBeatID, sender),
            getContext().system().dispatcher(),
            sender
        );
    }

    private void scheduleFlushTimeout(View view, ActorRef sender) {
        getContext().system().scheduler().scheduleOnce(
            Duration.ofMillis(TIMEOUT),
            getSelf(),
            new FlushTimeoutMessage(view, sender),
            getContext().system().dispatcher(),
            sender
        );
    }

    private void sendViewChange(View newView, ActorRef self) {
        multicastToView(new ViewChangeMessage(newView, actor2id), newView);
        getSelf().tell(new ViewChangeMessage(newView, null), self);
    }

    private void startViewChange(ActorRef timedOut, ActorRef self, String logMsg) {
        lastViewID++;

        View updatedView = new View(
            lastViewID,
            lastGeneratedView.members.stream()
                .filter((actor) -> !actor.equals(timedOut))
                .collect(Collectors.toList())
        );

        // Important! this update must be performed
        lastGeneratedView = updatedView;

//        logger.info(logMsg);
        sendViewChange(updatedView, self);
    }

    private void onStartMessage(StartMessage msg) {
        currentView = msg.view;
        lastGeneratedView = currentView;
        actor2id.put(getSelf(), id);

        setLogger(GroupManager.class.getName(), "group-manager.log");
//        logger.info("Group Manager initialized with view " + currentView.id);
    }

    private void onJoinMessage(JoinMessage msg) {
//        logger.info(getSender().path().name() + " requested to join the system");
        List<ActorRef> updatedMembers =
            new ArrayList<>(lastGeneratedView.members);
        updatedMembers.add(getSender());

        lastAssignedID++;
        actor2id.put(getSender(), lastAssignedID);
        getSender().tell(new AssignIDMessage(lastAssignedID), getSelf());
        scheduleTimeout(0, getSender());

        lastViewID++;
        View updatedView = new View(lastViewID, updatedMembers);
        lastGeneratedView = updatedView;
//        logger.info("Join triggered");
        sendViewChange(updatedView, getSelf());

        for (ActorRef actor : updatedView.members) {
            if (!actor.equals(getSelf())) {
                scheduleFlushTimeout(updatedView, actor);
            }
        }
    }

    private void onHeartbeatMessage(HeartbeatMessage msg) {
        lastBeat.put(getSender(), msg.id);
    }

    private void onTimeout(TimeoutMessage msg) {
        if (!ignoreTimeout.getOrDefault(getSender(), false)) {
            if (lastBeat.get(getSender()) <= msg.lastBeatID) {
                startViewChange(
                    getSender(),
                    getSelf(),
                    "Timeout triggered - " + getSender()
                );
            } else {
                scheduleTimeout(lastBeat.get(getSender()), getSender());
            }
        }
    }

    private void onFlushTimeout(FlushTimeoutMessage msg) {
        if (receivedFlush.containsKey(msg.view)) {
            if (!receivedFlush.get(msg.view).contains(getSender())) {
                // necessary to prevent subsequent timeout due to missing heartbeat
                // (the node is already crashed when a timeout is received after a flush timeout)
                ignoreTimeout.putIfAbsent(getSender(), true);

                startViewChange(
                    getSender(),
                    getSelf(),
                    "Flush Timeout triggered"
                );
            }
        }
    }
}
