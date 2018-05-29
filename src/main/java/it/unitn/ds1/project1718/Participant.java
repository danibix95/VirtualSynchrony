package it.unitn.ds1.project1718;

import akka.actor.Props;
import it.unitn.ds1.project1718.Messages.*;

import java.time.Duration;

public class Participant extends Node {
    private int messageID;
    private final int MAX_DELAY_BETWEEN_MSG = 2500;
    private final int MIN_DELAY_BETWEEN_MSG = 1500;
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
        .match(DataMessage.class, this::onDataMessage)
        .match(ViewChangeMessage.class, this::onViewChangeMessage)
        .match(FlushMessage.class, this::onFlushMessage)
        .match(StableMessage.class, this::onStableMessage)
        .match(SendDataMessage.class, this::onSendDataMessage)
        .match(CrashMessage.class, this::onCrashMessage)
        .match(A2AMessage.class, this::onA2AMessage)
        .build();
    }

    protected void onAssignIDMessage(AssignIDMessage msg) {
        this.id = msg.newID;
        this.actor2id = msg.actorMapping;
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
        if(!justEntered && !this.crashed) {
            super.onDataMessage(msg);
        }
    }

    @Override
    protected void onViewChangeMessage(ViewChangeMessage msg) {
        if(!this.crashed) {
            this.allowSending = false;
            super.onViewChangeMessage(msg);
        }
    }

    private void onSendDataMessage(SendDataMessage msg) {
        if(this.allowSending && !this.crashed){
            System.out.format("%d send multicast %d within %d\n",
                this.id, this.messageID, currentView.id);
            DataMessage dataMessage = new DataMessage(messageID, this.id);
            multicast(dataMessage);
            multicast(new StableMessage(dataMessage.id, this.id));
            this.messageID++;
            waitIntervalToSend();
        }
    }

    private int randomWatingTime() {
        return rnd.nextInt(MAX_DELAY_BETWEEN_MSG-MIN_DELAY_BETWEEN_MSG)+MIN_DELAY_BETWEEN_MSG;
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
        System.out.format("%d Crashed!\n",this.id);
    }
}
