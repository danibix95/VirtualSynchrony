package it.unitn.ds1.project1718;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import it.unitn.ds1.project1718.Messages.*;

import java.io.Serializable;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;
import java.util.stream.Collectors;

public abstract class Node extends AbstractActor {
    protected int id;
    protected View currentView;
    protected HashMap<View,List<DataMessage>> unstableMessages = new HashMap<>();
    protected TreeMap<View,List<ActorRef>> receivedFlush = new TreeMap<>();
    protected HashMap<View,List<DataMessage>> receivedMessages = new HashMap<>();
    protected HashMap<ActorRef, Integer> actor2id = new HashMap<>();
    protected HashMap<View,List<DataMessage>> waitToDeliver = new HashMap<>();

    protected Logger logger = null;

    protected Random rnd = new Random();

    public Node() {
        super();
    }

    public static class View implements Comparable<View> {
        public final int id;
        public final List<ActorRef> members;

        public View(int id, List<ActorRef> members) {
            this.id = id;
            this.members = Collections.unmodifiableList(members);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof View) {
                return ((View)o).id == this.id;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.id;
        }

        @Override
        public int compareTo(View v) {
            return this.id - v.id;
        }
    }

    protected static class TextFormatter extends Formatter {
        public String format(LogRecord record) {
            StringBuilder builder = new StringBuilder(1000);
            builder.append(formatMessage(record));
            builder.append("\n");
            return builder.toString();
        }

        public String getHead(Handler h) {
            return super.getHead(h);
        }

        public String getTail(Handler h) {
            return super.getTail(h);
        }
    }

    protected void setLogger(String className, String filename) {
        this.logger = Logger.getLogger(className);
        this.logger.setLevel(Level.INFO);
        FileHandler fh;
        try {
            fh = new FileHandler("vs-logs/" + filename);
            TextFormatter formatter = new TextFormatter();
            fh.setFormatter(formatter);
            this.logger.setUseParentHandlers(false);
            this.logger.addHandler(fh);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void multicast(Serializable m) {
        multicastToView(m, currentView);
    }

    protected void multicastToView(Serializable m, View view) {
        List<ActorRef> shuffledGroup = new ArrayList<>(view.members);
        Collections.shuffle(shuffledGroup);
        for (ActorRef p : shuffledGroup) {
            if (!p.equals(getSelf())) {
                p.tell(m, getSelf());
                try {
                    Thread.sleep(rnd.nextInt(10));
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private View getPreviousView(View v) {
        return new View(v.id-1, new ArrayList<>());
    }

    protected void onViewChangeMessage(ViewChangeMessage msg) {
        View previousView = getPreviousView(msg.view);
        if (unstableMessages.containsKey(previousView)) {
            sendAllUnstableMessages(unstableMessages.get(previousView), msg.view);
        }
        multicastToView(new FlushMessage(msg.view), msg.view);
        getSelf().tell(new FlushMessage(msg.view), getSelf());
    }

    protected void sendAllUnstableMessages(List<DataMessage> messages, View view) {
        for (DataMessage m: messages) {
            multicastToView(new A2AMessage(m.id, m.senderID), view);
        }
    }

    protected boolean onFlushMessage(FlushMessage msg) {
        View view = msg.view;
        if (!receivedFlush.containsKey(view)) {
            receivedFlush.put(view, new ArrayList<>());
        }
        receivedFlush.get(view).add(getSender());
        if (receivedFlush.get(view).containsAll(view.members)) {
            // install new view
            Iterator<Map.Entry<View,List<ActorRef>>> iter = receivedFlush.entrySet().iterator();
            while (iter.hasNext()) {
                View v = iter.next().getKey();
                // install all the "open" views up to the completely installed one
                if (v.compareTo(view) <= 0) {
                    logger.info(
                            this.id + " install view "
                            + v.id + " "
                            + v.members.stream()
                                .map((m) -> String.valueOf(actor2id.get(m)))
                                .collect(Collectors.joining(","))
                    );
                    this.currentView = v;
                    iter.remove();
                    unstableMessages.remove(v);
                    // deliver messages waiting for the viewInstall
                    List<DataMessage> queue = this.waitToDeliver.get(v);
                    if(queue!=null){
                        for(DataMessage m: queue){
                            //System.out.print("From Q ");
                            deliver(m);
                        }
                    }
                    this.waitToDeliver.remove(v);
                }
                else break;
            }
            return this.receivedFlush.isEmpty();
        }
        return false;
    }

    private View fromWhichView(ActorRef sender) {
        if (!receivedFlush.keySet().isEmpty()) {
            // find the last flush i've got from said process
            TreeMap<View,List<ActorRef>> rev = new TreeMap<>(Collections.reverseOrder());
            rev.putAll(receivedFlush);

            for (Map.Entry<View,List<ActorRef>> entry:rev.entrySet()) {
                List<ActorRef> recFlush = entry.getValue();

                if(recFlush.contains(sender)) {
                    // as soon as I find a flush(the latest) i return the view for which it has been sent
                    return entry.getKey();
                }
            }
            // if I don't find any flush from the given node -> still current view
        }
        // the same view; no view change has been triggered
        return this.currentView;
    }

    private void deliver(DataMessage msg) {
        logger.info(
            this.id + " deliver multicast "
                + msg.id + " from "
                + msg.senderID + " within "
                + currentView.id
        );
        if (!this.receivedMessages.containsKey(this.currentView)) {
            this.receivedMessages.put(this.currentView, new ArrayList<>());
        }
        this.receivedMessages.get(this.currentView).add(msg);
    }

    protected void onDataMessage(DataMessage msg) {
        View senderView = fromWhichView(getSender());
        if (senderView == this.currentView) {
            deliver(msg);
        }
        else {
            if (!this.waitToDeliver.containsKey(senderView)) {
                this.waitToDeliver.put(senderView, new ArrayList<>());
            }
            this.waitToDeliver.get(senderView).add(msg);
        }

        if (!this.unstableMessages.containsKey(senderView)) {
            this.unstableMessages.put(senderView, new ArrayList<>());
        }
        this.unstableMessages.get(senderView).add(msg);
    }

    protected void onStableMessage(StableMessage msg) {
        List<DataMessage> l= this.unstableMessages.get(fromWhichView(getSender()));
        if(l!=null){
            l.remove(new DataMessage(msg.messageID, msg.senderID));
        }
    }

    protected void onA2AMessage(A2AMessage msg) {
        ActorRef sender = getSender();
        View senderView = fromWhichView(sender);
        if (!receivedMessages.containsKey(senderView) && msg.senderID != this.id) {
            if (senderView == this.currentView) {
                deliver(msg);
            }
            else {
                this.waitToDeliver.get(senderView).add(msg);
            }
        }
    }
}
