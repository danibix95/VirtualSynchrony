package it.unitn.ds1.project1718;

import akka.actor.Props;
import it.unitn.ds1.project1718.Messages.*;

import java.time.Duration;

public class Participant extends Node {
    private int messageID;
    private final int MAX_DELAY_BETWEEN_MSG = 1000;
    private final int MIN_DELAY_BETWEEN_MSG = 300;
    private boolean justEntered;
    private boolean allowSending;

    public Participant(int id) {
        super(id);
        this.messageID = 0;
        this.justEntered = true;
        this.allowSending = false;
    }

    public static Props props(int id) {
    	return Props.create(Participant.class, () -> new Participant(id));
    }

    @Override
    public Receive createReceive() {
      return receiveBuilder()
        .match(DataMessage.class, this::onDataMessage)
        .match(ViewChangeMessage.class, this::onViewChangeMessage)
        .match(FlushMessage.class, this::onFlushMessage)
        .match(StableMessage.class, this::onStableMessage)
        .match(SendDataMessage.class, this::onSendDataMessage)
        .build();
    }

    @Override
    protected boolean onFlushMessage(FlushMessage msg){
        boolean viewInstalled = super.onFlushMessage(msg);
        if(viewInstalled) {
            this.justEntered = false;
            this.allowSending = true;
            return true;
        }
        return false;
    }

    @Override
    protected void onDataMessage(DataMessage msg) {
        if(!justEntered) {
            super.onDataMessage(msg);
        }
    }

    @Override
    protected void onViewChangeMessage(ViewChangeMessage msg) {
        this.allowSending = false;
        super.onViewChangeMessage(msg);
    }

    private void onSendDataMessage(SendDataMessage msg) {
        if(this.allowSending){
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
}
