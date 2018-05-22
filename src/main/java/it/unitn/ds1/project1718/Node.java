package it.unitn.ds1.project1718;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import it.unitn.ds1.project1718.Messages.StartMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Random;

public abstract class Node extends AbstractActor {
    protected int id;
    protected List<ActorRef> participants;

    protected rnd = new Random();

    public Node(int id) {
        super();
        this.id = id;
    }

    void setGroup(StartMessage sm) {
        participants = new ArrayList<ActorRef>();
        for (ActorRef actor: sm.groupMembers) {
            if (!actor.equals(getSelf())) {
                this.participants.add(actor);
            }
        }
    }

    private void multicast(Serializable m) {
        List<ActorRef> shuffledGroup = new ArrayList<>(participants);
        Collections.shuffle(shuffledGroup);
        for(ActorRef p:shuffledGroup){
            if(!p.equals(getSelf())) {
                p.tell(m,getSelf());
                try {
                    Thread.sleep(rnd.nextInt(10));
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }

}
