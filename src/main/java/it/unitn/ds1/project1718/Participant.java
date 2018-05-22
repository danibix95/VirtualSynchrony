package it.unitn.ds1.project1718;

import it.unitn.ds1.project1718.Messages.*;
import java.util.HashSet;

public class Participant extends Node {

	private ActorRef groupManager;

    public Participant(int id) {
        super(id);
    }

    public static Props props() {
    	return Props.create(Participant.class, () -> new Participant());
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

    public void onDataMessage(DataMessage msg){

    }

    public void onFlushMessage(FlushMessage msg){
    	view = msg.view;
    	if(!receivedFlush.containsKey(view)){
    		receivedFlush.put(view,new List<ActorRef>());
    	}
    	receivedFlush.get(view).add(getSender());
    	if(receivedFlush.get(view).containsAll(view)){
    		// install new view 
    		participants = view;
    		receivedFlush.remove(view);
    	}
    }

    public void onUnstableSharingMessage(UnstableSharingMessage msg){

    }

    public void onStableMessage(StableMessage msg){

    }

    public void onJoinMessage(JoinMessage msg){

    }
}
