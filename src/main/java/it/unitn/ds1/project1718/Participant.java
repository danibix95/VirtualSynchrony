package it.unitn.ds1.project1718;

import akka.actor.ActorRef;
import akka.actor.Props;
import it.unitn.ds1.project1718.Messages.*;

import java.io.Serializable;
import java.time.Duration;

public class Participant extends Node {
    private int messageID;
    private final int MAX_DELAY = 2000;
    private final int MIN_DELAY = 1200;
    private ActorRef groupManager;
    private boolean justEntered;
    private boolean allowSending;
    private boolean crashed;
    private boolean crashSending = false;
    private boolean crashReceiving = false;
    private boolean crashOnViewChange = false;

    public Participant() {
        super();
        this.messageID = 0;
        this.justEntered = true;
        this.allowSending = false;
        this.crashed = false;
    }

    public static Props props() {
    	return Props.create(Participant.class, Participant::new);
    }

    @Override
    public Receive createReceive() {
      return receiveBuilder()
        .match(AssignIDMessage.class, this::onAssignIDMessage)
        .match(SendDataMessage.class, this::onSendDataMessage)
        .match(A2AMessage.class, this::onA2AMessage)        // binding of A2A MUST be before DataMessage
        .match(DataMessage.class, this::onDataMessage)      // hierarchical overshadowing
        .match(StableMessage.class, this::onStableMessage)
        .match(ViewChangeMessage.class, this::onViewChangeMessage)
        .match(FlushMessage.class, this::onFlushMessage)
        .match(HeartbeatMessage.class, this::onHearthbeatMessage)
        .match(CrashSendingMessage.class, this::onCrashWhileSendingMessage)
        .match(CrashReceivingMessage.class, this::onCrashAfterReceiveMessage)
        .match(CrashOnViewChangeMessage.class, this::onCrashOnViewChangeMessage)
        .match(CrashMessage.class, this::onCrashMessage)    // this must stay after other crash messages (see above)
        .match(ExitMessage.class, this::onExitMessage)
        .build();
    }

    /** Send a multicast only to half of the current group
     *  and then set itself as being crashed.
     * */
    private void multicastCrashing(Serializable m) {
        View subsetView = new View(
            currentView.id,
            currentView.members.subList(0, currentView.members.size()/2)
        );
        multicastToView(m, subsetView);
        crashed = true;
    }

    private int randomWaitingTime() {
        return rnd.nextInt(MAX_DELAY - MIN_DELAY) + MIN_DELAY;
    }

    private void waitIntervalToSend(Serializable msg, int interval) {
        getContext().system().scheduler().scheduleOnce(
            Duration.ofMillis(interval),
            getSelf(),
            msg,
            getContext().system().dispatcher(),
            getSelf()
        );
    }

    private void onAssignIDMessage(AssignIDMessage msg) {
        this.id = msg.newID;
        groupManager = getSender();
        // start managing heartbeat messages
        getSelf().tell(new HeartbeatMessage(0), getSelf());

        setLogger(Participant.class.getName() + "-" + msg.newID,
            "node-" + msg.newID + ".log");
    }

    private void onSendDataMessage(SendDataMessage msg) {
        if (!crashed) {
            if (allowSending) {
                logger.info(this.id + " send multicast "
                    + this.messageID + " within " + currentView.id);
                DataMessage dataMessage = new DataMessage(messageID, this.id);

                if (crashSending) {
                    multicastCrashing(dataMessage);
                }
                else {
                    multicast(dataMessage);
                    multicast(new StableMessage(this.messageID, this.id));
                }
                this.messageID++;

                waitIntervalToSend(new SendDataMessage(), randomWaitingTime());
            }
        }
    }

    @Override
    protected void onDataMessage(DataMessage msg) {
        if (!crashed) {
            if (crashReceiving) {
                crashed = true;
            }
            else {
                super.onDataMessage(msg);
            }

        }
    }

    @Override
    protected void onStableMessage(StableMessage msg) {
        if (!crashed) super.onStableMessage(msg);
    }

    @Override
    protected void onA2AMessage(A2AMessage msg) {
        if (!justEntered && !crashed) super.onA2AMessage(msg);
    }

    @Override
    protected void onViewChangeMessage(ViewChangeMessage msg) {
        if (!crashed) {
            allowSending = false;
            msg.actorMapping.forEach((k, v) -> actor2id.put(k, v));

            if (crashOnViewChange) {
                crashed = true;
            }
            else {
                super.onViewChangeMessage(msg);
            }
        }
    }

    @Override
    protected boolean onFlushMessage(FlushMessage msg){
        if (!crashed) {
            boolean allViewInstalled = super.onFlushMessage(msg);
            if (allViewInstalled) {
                allowSending = true;
                // start sending data messages again
                waitIntervalToSend(new SendDataMessage(), randomWaitingTime());

                if (justEntered) {
                    justEntered = false;
                    getSelf().tell(new SendDataMessage(), getSelf());
                }
                return true;
            }
        }
        return false;
    }

    private void onHearthbeatMessage(HeartbeatMessage msg) {
        if (!crashed) {
            groupManager.tell(new HeartbeatMessage(msg.id), getSelf());
            waitIntervalToSend(new HeartbeatMessage(msg.id+1), HB_INTERVAL);
        }
    }

    private void onCrashMessage(CrashMessage msg) {
        crashed = true;
//        logger.info(this.id + " crashed!");
    }

    private void onCrashWhileSendingMessage(CrashSendingMessage msg) {
        crashSending = true;
//        logger.info(this.id + " going to crash on next multicast!");
    }

    private void onCrashAfterReceiveMessage(CrashReceivingMessage msg) {
        crashReceiving = true;
//        logger.info(this.id + " going to crash on next receiving!");
    }

    private void onCrashOnViewChangeMessage(CrashOnViewChangeMessage msg) {
        crashOnViewChange = true;
//        logger.info(this.id + " going to crash on next view change!");
    }

    private void onExitMessage(ExitMessage msg) {
        allowSending = false;
    }
}
