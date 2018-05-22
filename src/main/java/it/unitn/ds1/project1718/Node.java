package it.unitn.ds1.project1718;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class StartMessage implements Serializable {
    public final List<ActorRef> groupMembers;
    public StartMessage(List<ActorRef> group) {
        this.groupMembers =
            Collections.unmodifiableList(new ArrayList<ActorRef>(group));
    }
}

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
