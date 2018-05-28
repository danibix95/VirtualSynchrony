package it.unitn.ds1.project1718;

import akka.actor.Props;
import it.unitn.ds1.project1718.Messages.*;

import java.time.Duration;

public class Participant extends Node {
    private int messageID;
    private final int MAX_DELAY_BETWEEN_MSG = 1000;
    private final int MIN_DELAY_BETWEEN_MSG = 300;
    private boolean justEntered;

    public Participant() {
        super();
        messageID = -1;
        justEntered = true;
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
        .build();
    }

    protected void onAssignIDMessage(AssignIDMessage msg) {
        this.id = msg.newID;
    }

    protected boolean onFlushMessage(FlushMessage msg){
        boolean viewHasChanged = super.onFlushMessage(msg);
        if(viewHasChanged) {
            justEntered = false;
            return true;
        }
        return false;
    }
    
    protected void onDataMessage(DataMessage msg) {
        if(!justEntered) {
            super.onDataMessage(msg);
        }
    }

    private void onSendDataMessage(SendDataMessage msg) {
        System.out.format("%d send multicast %d within %d\n",
                          this.id, this.messageID, currentView.id);
        DataMessage dataMessage = new DataMessage(messageID, this.id);
        multicast(dataMessage);
        multicast(new StableMessage(dataMessage.id, this.id));
        messageID++;
        waitIntervalToSend();
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
}
