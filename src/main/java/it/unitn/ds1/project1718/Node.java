package it.unitn.ds1.project1718;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import it.unitn.ds1.project1718.Messages.*;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public abstract class Node extends AbstractActor {
    protected int id;
    protected View currentView;
    protected HashMap<View,List<DataMessage>> unstableMessages = new HashMap<>();
    protected TreeMap<View,List<ActorRef>> receivedFlush = new TreeMap<>();
    protected HashMap<View,List<DataMessage>> receivedMessages = new HashMap<>();

    protected Random rnd = new Random();

    public Node() {
        super();
    }

    public static class View implements Comparable<View>{
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

    protected void multicast(Serializable m) {
        multicastToView(m, currentView);
    }

    protected void multicastToView(Serializable m, View view) {
        List<ActorRef> shuffledGroup = new ArrayList<>(view.members);
        Collections.shuffle(shuffledGroup);
        for (ActorRef p:shuffledGroup) {
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
        return new View(v.id-1,new ArrayList<>());
    }

    protected void onViewChangeMessage(ViewChangeMessage msg) {
        View previousView = getPreviousView(msg.view);
        if(unstableMessages.containsKey(previousView)) {
            sendAllUnstableMessages(unstableMessages.get(previousView), msg.view);
        }
        multicastToView(new FlushMessage(msg.view), msg.view);
        getSelf().tell(new FlushMessage(msg.view), getSelf());
    }

    protected void sendAllUnstableMessages(List<DataMessage> messages, View view) {
        for (Serializable m: messages) {
            multicastToView(m,view);
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
                if (v.compareTo(view)<=0) {
                    System.out.format(
                            "%d install view %d %s\n",
                            this.id,
                            v.id,
                            v.members.stream().map((m) -> m.path().name())
                                    .collect(Collectors.joining(","))
                    );
                    currentView = v;
                    receivedFlush.remove(v);
                    unstableMessages.remove(v);
                }
                else break;
            }
            return true;
        }
        return false;
    }

    protected void onDataMessage(DataMessage msg) {
        System.out.format(
            "%d deliver multicast %d from %d within %d\n",
            this.id,
            msg.id,
            msg.senderID,
            currentView.id
        );
        if (!this.receivedMessages.containsKey(currentView)) {
            this.receivedMessages.put(currentView, new ArrayList<>());
        }
        this.receivedMessages.get(currentView).add(msg);
        if(!this.unstableMessages.containsKey(currentView)){
            this.unstableMessages.put(currentView,new ArrayList<>());
        }
        this.unstableMessages.get(currentView).add(msg);
    }

    protected void onStableMessage(StableMessage msg) {
        this.unstableMessages.remove(
            new DataMessage(msg.messageID, msg.senderID)
        );
    }
}
