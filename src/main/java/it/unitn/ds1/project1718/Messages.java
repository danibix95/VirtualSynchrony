package it.unitn.ds1.project1718;

import akka.actor.ActorRef;
import it.unitn.ds1.project1718.Node.View;

import java.io.Serializable;
import java.util.Collections;

public class Messages {
    public static class StartMessage implements Serializable {
        public final View view;
        public StartMessage(ActorRef groupManager) {
            this.view = new View(0, Collections.singletonList(groupManager));
        }
    }

    public static class AssignIDMessage implements Serializable {
        public final int newID;
        public AssignIDMessage(int newID) {
            this.newID = newID;
        }
    }

    public static class DataMessage implements Serializable {
        public final int id;
        public final int senderID;
        public DataMessage(int id, int originalSender) {
            this.id = id;
            this.senderID = originalSender;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof  DataMessage) {
                return ((DataMessage)o).id == this.id
                    && ((DataMessage)o).senderID == this.senderID;
            }
            return false;
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

    public static class StableMessage implements Serializable {
        public final int messageID;
        public final int senderID;
        public StableMessage(int messageID, int senderID) {
            this.messageID = messageID;
            this.senderID = senderID;
        }
    }

    public static class JoinMessage implements Serializable {}

    public static abstract class Timeout implements Serializable {
        public final ActorRef senderID;
        public Timeout(ActorRef senderID) {
            this.senderID = senderID;
        }
    }

    public static class TimeoutMessage extends Timeout {
        public final int checkID;
        public TimeoutMessage(int id, ActorRef messageSender) {
            super(messageSender);
            this.checkID = id;
        }
    }

    public static class FlushTimeoutMessage extends Timeout {
        public final View view;
        public FlushTimeoutMessage (View view, ActorRef messageSender) {
            super(messageSender);
            this.view = view;
        }
    }

    public static class SendDataMessage implements Serializable {}
}
