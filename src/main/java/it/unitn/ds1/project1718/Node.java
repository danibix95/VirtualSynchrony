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
    protected Set<DataMessage> unstableMessages = new HashSet<>();
    protected HashMap<View,List<ActorRef>> receivedFlush = new HashMap<>();
    protected HashMap<View,List<DataMessage>> receivedMessages = new HashMap<>();

    protected Random rnd = new Random();

    public Node(int id) {
        super();
        this.id = id;
    }

    public static class View {
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

    protected void onViewChangeMessage(ViewChangeMessage msg) {
        sendAllUnstableMessages(msg.view);
        multicastToView(new FlushMessage(msg.view), msg.view);
        getSelf().tell(new FlushMessage(msg.view), getSelf());
    }

    protected void sendAllUnstableMessages(View newView) {
        for (Serializable m : unstableMessages) {
            multicastToView(m, newView);
        }
    }

    protected void onFlushMessage(FlushMessage msg) {
        View view = msg.view;
        if (!receivedFlush.containsKey(view)) {
            receivedFlush.put(view, new ArrayList<ActorRef>());
        }
        receivedFlush.get(view).add(getSender());
        if (receivedFlush.get(view).containsAll(view.members)) {
            // install new view
             System.out.format(
                "%d install view %d %s",
                this.id,
                view.id,
                view.members.stream().map((m) -> m.path().name())
                    .collect(Collectors.joining(","))
            );
            currentView = view;
            receivedFlush.remove(view);
        }
    }

    protected void onDataMessage(DataMessage msg) {
        System.out.format(
            "%d deliver multicast %d from %d within %d",
            this.id,
            msg.id,
            msg.originalSender,
            currentView.id
        );
        if (!this.receivedMessages.containsKey(currentView)) {
            this.receivedMessages.put(currentView, new ArrayList<>());
        }
        this.receivedMessages.get(currentView).add(msg);
        this.unstableMessages.add(msg);
    }

    protected void onStableMessage(StableMessage msg) {
        this.unstableMessages.remove(
            new DataMessage(msg.messageID, msg.senderID)
        );
    }
}
