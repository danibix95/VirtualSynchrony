package it.unitn.ds1.project1718;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.unitn.ds1.project1718.Node.View;

public class Messages {
    public static class StartMessage implements Serializable {
        public final List<ActorRef> groupMembers;
        public StartMessage(List<ActorRef> group) {
            this.groupMembers =
                Collections.unmodifiableList(new ArrayList<ActorRef>(group));
        }
    }

    public static class DataMessage implements Serializable {
        public final int id;
        public DataMessage(int id) {
            this.id = id;
        }
    }

    public static class ViewChangeMessage implements Serializable {
		public final View view;
        public ViewChangeMessage(View view) {
            this.view = view;
        }    	
    }

    public static class FlushMessage implements Serializable {
    	public final View view;
        public FlushMessage(View view) {
            this.view = view;           
        }
    }

    public static class UnstableSharingMessage implements Serializable {}
    public static class StableMessage implements Serializable {}
    public static class JoinMessage implements Serializable {}

    public static class TimeoutMessage implements Serializable {
        public final int checkId;
        public TimeoutMessage(int id) {
            this.checkId = id;
        }
    }
}
