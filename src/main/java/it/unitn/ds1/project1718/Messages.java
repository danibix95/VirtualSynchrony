package it.unitn.ds1.project1718;

import akka.actor.ActorRef;
import it.unitn.ds1.project1718.Node.View;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;

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
            if (o instanceof DataMessage) {
                return ((DataMessage)o).id == this.id
                    && ((DataMessage)o).senderID == this.senderID;
            }
            return false;
        }
    }

    // message used during data exchange before flush message is sent
    public static class A2AMessage extends DataMessage {
        public A2AMessage(int id, int originalSender) {
            super(id, originalSender);
        }
    }

    public static class ViewChangeMessage implements Serializable {
		public final View view;
        public final HashMap<ActorRef, Integer> actorMapping;
        public ViewChangeMessage(View view, HashMap<ActorRef, Integer> actorMapping) {
            this.view = view;
            this.actorMapping = actorMapping;
        }    	
    }

    public static class FlushMessage implements Serializable {
    	public final View view;
        public FlushMessage(View view) {
            this.view = view;           
        }
    }

    public static class StableMessage implements Serializable {
        public final int id;
        public final int messageID;
        public final int senderID;
        public StableMessage(int id,int messageID, int senderID) {
            this.id = id;
            this.messageID = messageID;
            this.senderID = senderID;
        }
    }

    public static class JoinMessage implements Serializable {}

    public static abstract class Timeout implements Serializable {
        public final ActorRef sender;
        public Timeout(ActorRef sender) {
            this.sender = sender;
        }
    }

    public static class TimeoutMessage extends Timeout {
        public int lastBeatID;
        public TimeoutMessage(int lastBeatID, ActorRef messageSender) {
            super(messageSender);
            this.lastBeatID = lastBeatID;
        }
    }

    public static class FlushTimeoutMessage extends Timeout {
        public final View view;
        public FlushTimeoutMessage (View view, ActorRef messageSender) {
            super(messageSender);
            this.view = view;
        }
    }

    public static class HeartbeatMessage implements Serializable {
        public int id;

        public HeartbeatMessage(int id) {
            this.id = id;
        }
    }

    public static class SendDataMessage implements Serializable {}

    public static class CrashMessage implements Serializable {
        public String info;

        public CrashMessage(String info) {
            this.info = info;
        }
    }

    public static class CrashSendingMessage extends CrashMessage {
        public CrashSendingMessage(String info) {
            super(info);
        }
    }

    public static class CrashReceivingMessage extends CrashMessage {
        public CrashReceivingMessage(String info) {
            super(info);
        }
    }

    public static class CrashOnViewChangeMessage extends CrashMessage {
        public CrashOnViewChangeMessage(String info) {
            super(info);
        }
    }
}
