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
    private final static int MULTICAST_TIMEOUT = 5500;
    private HashMap<ActorRef, Integer> lastMessages = new HashMap<>();
    private View lastGeneratedView;
    // this variable control if data timeouts must be rescheduled
    // once a flush message is received
    private boolean rescheduleTimeouts = false;

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
            .match(StableMessage.class, this::onStableMessage)
            .match(TimeoutMessage.class, this::onTimeout)
            .match(FlushTimeoutMessage.class, this::onFlushTimeout)
            .match(ViewChangeMessage.class, this::onViewChangeMessage)
            .match(FlushMessage.class, this::onFlushMessage)
            .match(JoinMessage.class, this::onJoinMessage)
            .match(A2AMessage.class, this::onA2AMessage)        // binding of A2A MUST be before DataMessage
            .match(DataMessage.class, this::onDataMessage)      // hierarchical overshadowing
            .build();
    }

    private void scheduleTimeout(int messageID, ActorRef sender) {
        getContext().system().scheduler().scheduleOnce(
            Duration.ofMillis(MULTICAST_TIMEOUT),
            getSelf(),
            new TimeoutMessage(messageID, sender),
            getContext().system().dispatcher(),
            sender
        );
    }

    private void scheduleFlushTimeout(View view, ActorRef sender) {
        getContext().system().scheduler().scheduleOnce(
            Duration.ofMillis(MULTICAST_TIMEOUT),
            getSelf(),
            new FlushTimeoutMessage(view, sender),
            getContext().system().dispatcher(),
            sender
        );
    }

    private void onStartMessage(StartMessage msg) {
        currentView = msg.view;
        lastGeneratedView = currentView;
        actor2id.put(getSelf(), id);

        setLogger(GroupManager.class.getName(), "group-manager.log");
        logger.info("Group Manager initialized with view " + currentView.id);
    }

    @Override
    protected boolean onFlushMessage(FlushMessage msg){
        boolean allViewInstalled = super.onFlushMessage(msg);
        if (allViewInstalled) {
            rescheduleTimeouts = false;
            return true;
        }
        return false;
    }

    protected void onDataMessage(DataMessage msg) {
        super.onDataMessage(msg);
        lastMessages.put(getSender(), msg.id);
        scheduleTimeout(msg.id, getSender());
    }

    protected void onStableMessage(StableMessage msg) {
        super.onStableMessage(msg);
        lastMessages.put(getSender(), msg.id);
        scheduleTimeout(msg.id, getSender());
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

        logger.info(logMsg);
        sendViewChange(updatedView, self);
    }

    private void onTimeout(TimeoutMessage msg) {
        if (!rescheduleTimeouts) {
            // notice: the sender of this message was set as it was sent by the timed out node
            if (lastMessages.getOrDefault(getSender(), -1) == msg.checkID) {
                startViewChange(
                    getSender(),
                    getSelf(),
                    "Timeout triggered - " + getSender() + ":" + msg.checkID
                );
            }
        }
        else {
            // reschedule again the timeout for this message
            // in case a flush timeout was triggered
            scheduleTimeout(msg.checkID, getSender());
        }
    }

    private void onFlushTimeout(FlushTimeoutMessage msg) {
        if (receivedFlush.containsKey(msg.view)) {
            if (!receivedFlush.get(msg.view).contains(getSender())) {
                rescheduleTimeouts = true;
                startViewChange(
                    getSender(),
                    getSelf(),
                    "Flush Timeout triggered"
                );
            }
        }
    }

    private void onJoinMessage(JoinMessage msg) {
        logger.info(getSender().path().name() + " requested to join the system");
        List<ActorRef> updatedMembers =
            new ArrayList<>(lastGeneratedView.members);
        updatedMembers.add(getSender());

        lastAssignedID++;
        actor2id.put(getSender(), lastAssignedID);
        getSender().tell(new AssignIDMessage(lastAssignedID), getSelf());

        lastViewID++;
        View updatedView = new View(lastViewID, updatedMembers);
        lastGeneratedView = updatedView;
        logger.info("Join triggered");
        sendViewChange(updatedView, getSelf());

        for (ActorRef actor : updatedView.members) {
            if (!actor.equals(getSelf())) {
                scheduleFlushTimeout(updatedView, actor);
            }
        }
    }
}
