package it.unitn.ds1.project1718;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.unitn.ds1.project1718.Messages.JoinMessage;
import it.unitn.ds1.project1718.Messages.StartMessage;

public class VirtualSynchrony {
    private final static int PARTICIPANTS = 3;

    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create("virtual-synchrony");

        ActorRef groupManager = system.actorOf(GroupManager.props(), "group-manager");

        // initial ID for group currentView
        int id = 1;
        List<ActorRef> initialGroup = new ArrayList<>();
        for (; id <= PARTICIPANTS; id++) {
            initialGroup.add(system.actorOf(
                Participant.props(id),
                String.valueOf(id))
            );
        }
        initialGroup = Collections.unmodifiableList(initialGroup);

        // insert the group manager in the initial view
        groupManager.tell(new StartMessage(groupManager), ActorRef.noSender());

        for (ActorRef member : initialGroup) {
            groupManager.tell(new JoinMessage(), member);
        }

        try {
            System.out.println("\n>>> Press ENTER to exit <<<\n");
            System.in.read();
        }
        catch (IOException ioe) {}
        system.terminate();
    }
}
