package it.unitn.ds1.project1718;

import it.unitn.ds1.project1718.Messages.*;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import akka.actor.ActorRef;
import akka.actor.Props;

public class Participant extends Node {

    public Participant(int id) {
        super(id);
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
        .match(UnstableSharingMessage.class, this::onUnstableSharingMessage)
        .match(StableMessage.class, this::onStableMessage)
        .build();
    }

    public void onDataMessage(DataMessage msg){

    }

    public void onFlushMessage(FlushMessage msg){
    	List<ActorRef> view = msg.view;
    	if(!receivedFlush.containsKey(view)){
    		receivedFlush.put(view,new ArrayList<ActorRef>());
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
}
