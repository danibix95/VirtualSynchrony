package it.unitn.ds1.project1718;

import akka.actor.Props;
import it.unitn.ds1.project1718.Messages.*;

import java.time.Duration;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Participant extends Node {
    private int messageID;
    private final int MAX_DELAY = 2500;
    private final int MIN_DELAY = 1500;
    private boolean justEntered;
    private boolean allowSending;
    private boolean crashed;

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
        .match(ViewChangeMessage.class, this::onViewChangeMessage)
        .match(FlushMessage.class, this::onFlushMessage)
        .match(StableMessage.class, this::onStableMessage)
        .match(SendDataMessage.class, this::onSendDataMessage)
        .match(CrashMessage.class, this::onCrashMessage)
        .match(A2AMessage.class, this::onA2AMessage)            // binding of A2A MUST be before DataMessage
        .match(DataMessage.class, this::onDataMessage)          // hierarchical overshadowing
        .build();
    }

    protected void onAssignIDMessage(AssignIDMessage msg) {
        this.id = msg.newID;
        this.actor2id = msg.actorMapping;

        setLogger(Participant.class.getName() + "-" + msg.newID,
            "node-" + msg.newID + ".log");
    }

    @Override
    protected boolean onFlushMessage(FlushMessage msg){
        if(!this.crashed) {
            boolean viewInstalled = super.onFlushMessage(msg);
            if(viewInstalled) {
                this.allowSending = true;
                if(this.justEntered){
                    this.justEntered = false;
                    getSelf().tell(new SendDataMessage(),getSelf());
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onStableMessage(StableMessage msg) {
        if(!this.crashed) {
            super.onStableMessage(msg);
        }
    }

    @Override
    protected void onDataMessage(DataMessage msg) {
        if(!this.crashed) {
            super.onDataMessage(msg);
        }
    }

    @Override
    protected void onA2AMessage(A2AMessage msg) {
        if (!justEntered) super.onA2AMessage(msg);
    }

    @Override
    protected void onViewChangeMessage(ViewChangeMessage msg) {
        if (!this.crashed) {
            this.allowSending = false;
            super.onViewChangeMessage(msg);
        }
    }

    private void onSendDataMessage(SendDataMessage msg) {
        // TODO: if not crashed delay event
        if (this.allowSending && !this.crashed) {
            logger.info(this.id + " send multicast "
                    + this.messageID + " within " + currentView.id);
            DataMessage dataMessage = new DataMessage(messageID, this.id);
            multicast(dataMessage);
            this.messageID++;

            multicast(new StableMessage(this.messageID, dataMessage.id, this.id));
            this.messageID++;
            waitIntervalToSend();
        }
    }

    private int randomWatingTime() {
        return rnd.nextInt(MAX_DELAY - MIN_DELAY) + MIN_DELAY;
    }

    private void waitIntervalToSend() {
        getContext().system().scheduler().scheduleOnce(
                Duration.ofMillis(randomWatingTime()),
                getSelf(),
                new SendDataMessage(),
                getContext().system().dispatcher(),
                getSelf()
        );
    }

    private void onCrashMessage(CrashMessage msg) {
        this.crashed = true;
        logger.info(this.id + " crashed!");
    }
}
