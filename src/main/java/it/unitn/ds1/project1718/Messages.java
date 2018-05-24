package it.unitn.ds1.project1718;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.unitn.ds1.project1718.Node.View;

import javax.xml.crypto.Data;

public class Messages {

    public static class DataMessage implements Serializable {
        public final int id;
        public final ActorRef originalSender;
        public DataMessage(int id,ActorRef originalSender) {
            this.id = id;
            this.originalSender = originalSender;
        }

        @Override
        public boolean equals(Object o) {
            if(o instanceof  DataMessage) {
                return ((DataMessage)o).id == this.id && ((DataMessage)o).originalSender == this.originalSender;
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

    public static class UnstableSharingMessage implements Serializable {}
    public static class StableMessage implements Serializable {
        public final int messageID;
        public StableMessage(int messageID) {
            this.messageID = messageID;
        }
    }
    public static class JoinMessage implements Serializable {}

    public static class TimeoutMessage implements Serializable {
        public final int checkId;
        public TimeoutMessage(int id) {
            this.checkId = id;
        }
    }

    public static class SendDataMessage implements Serializable {}
}
