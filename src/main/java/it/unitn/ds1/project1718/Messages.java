package it.unitn.ds1.project1718;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Messages {
    public static class StartMessage implements Serializable {
        public final List<ActorRef> groupMembers;
        public StartMessage(List<ActorRef> group) {
            this.groupMembers =
                Collections.unmodifiableList(new ArrayList<ActorRef>(group));
        }
    }
}
