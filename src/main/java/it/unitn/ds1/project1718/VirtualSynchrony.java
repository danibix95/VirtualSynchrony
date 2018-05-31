package it.unitn.ds1.project1718;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.unitn.ds1.project1718.Messages.JoinMessage;
import it.unitn.ds1.project1718.Messages.StartMessage;
import it.unitn.ds1.project1718.Messages.CrashMessage;

public class VirtualSynchrony {
    private final static int PARTICIPANTS = 3;

    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create("virtual-synchrony");

        ActorRef groupManager = system.actorOf(GroupManager.props(), "0");

        // initial ID for group currentView
        int id = 1;
        List<ActorRef> initialGroup = new ArrayList<>();
        for (; id <= PARTICIPANTS; id++) {
            initialGroup.add(system.actorOf(
                Participant.props(),
                String.valueOf("node-" + id))
            );
        }
        initialGroup = Collections.unmodifiableList(initialGroup);

        // insert the group manager in the initial view
        groupManager.tell(new StartMessage(groupManager), ActorRef.noSender());

        for (ActorRef member : initialGroup) {
            groupManager.tell(new JoinMessage(), member);
        }

        try {
            System.out.println("\n>>> Press ENTER to crash node 1 <<<\n");
            System.in.read();
            System.out.println("\tCrashing node 1");
            initialGroup.get(0).tell(new CrashMessage(),ActorRef.noSender());
            System.out.println("\n>>> Press ENTER to exit <<<\n");
            System.in.read();

            // join     J
            // crash    C x
            // recover  R x
        }
        catch (IOException ioe) {}
        system.terminate();
    }
}
