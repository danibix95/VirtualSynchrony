package it.unitn.ds1.project1718;

import it.unitn.ds1.project1718.Messages.*;

public class Partecipant extends Node {

	private ActorRef groupManager;

    public Partecipant(int id) {
        super(id);
    }

    public static Props props() {
    	return Props.create(Partecipant.class, () -> new Partecipant());
    }

    @Override
    public Receive createReceive() {
      return receiveBuilder()
        .match(DataMessage.class, this::onDataMessage)
        .match(ViewChangeMessage.class, this::onViewChangeMessage)
        .match(FlushMessage.class, this::onFlushMessage)
        .match(UnstableSharingMessage.class, this::onUnstableSharingMessage)
        .match(StableMessage.class, this::onStableMessage)
        .match(JoinMessage.class, this::onJoinMessage)
        .build();
    }

    public void onDataMessage(){

    }

    public void onViewChangeMessage(ViewChangeMessage msg){
    	//groupManager = getSender();		// only the GroupManager will send this kind of messages
    	
    }

    public void onFlushMessage(){

    }

    public void onUnstableSharingMessage(){

    }

    public void onStableMessage(){

    }

    public void onJoinMessage(){

    }
}
