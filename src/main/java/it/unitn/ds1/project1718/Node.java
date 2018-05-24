package it.unitn.ds1.project1718;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import it.unitn.ds1.project1718.Messages.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

public abstract class Node extends AbstractActor {
    protected int id;
    protected View participants;
    protected Set<Serializable> unstableMessages = new HashSet<>();
    protected HashMap<View,List<ActorRef>> receivedFlush = new HashMap<>();

    protected Random rnd = new Random();

    public Node(int id) {
        super();
        this.id = id;
    }

    public static class View {
        public final int id;
        public final List<ActorRef> members;

        public View(int id, List<ActorRef> members) {
            this.id = id;
            this.members = members;
        }

        @Override
        public boolean equals(Object o) {
            if(o instanceof View) {
                return ((View)o).id == this.id;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.id;
        }
    }

//    void setGroup(StartMessage sm) {
//        participants = new ArrayList<ActorRef>();
//        for (ActorRef actor : sm.groupMembers) {
//            if (!actor.equals(getSelf())) {
//                this.participants.add(actor);
//            }
//        }
//    }
    
    public abstract void onDataMessage(DataMessage msg);

    protected void multicast(Serializable m) {
        multicastToView(m, participants);
    }

    protected void multicastToView(Serializable m, View view) {
        List<ActorRef> shuffledGroup = new ArrayList<>(view.members);
        Collections.shuffle(shuffledGroup);
        for (ActorRef p:shuffledGroup) {
            if (!p.equals(getSelf())) {
                p.tell(m,getSelf());
                try {
                    Thread.sleep(rnd.nextInt(10));
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public Receive createReceive() {
      return receiveBuilder().build();
    }

    public void onViewChangeMessage(ViewChangeMessage msg){
        sendAllUnstableMessages(); // TODO: send unstable to the new view
        multicastToView(new FlushMessage(msg.view), msg.view);
        getSelf().tell(new FlushMessage(msg.view), getSelf());
    }

    protected void sendAllUnstableMessages() {
        for (Serializable m : unstableMessages) {
            multicast(m);
        }
    }

}
