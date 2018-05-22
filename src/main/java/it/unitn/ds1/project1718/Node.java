package it.unitn.ds1.project1718;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import it.unitn.ds1.project1718.Messages.StartMessage;

import java.util.ArrayList;
import java.util.List;

public abstract class Node extends AbstractActor {
    protected int id;
    protected List<ActorRef> participants;

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
}
