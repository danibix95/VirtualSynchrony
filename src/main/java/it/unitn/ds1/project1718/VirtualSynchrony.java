package it.unitn.ds1.project1718;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.unitn.ds1.project1718.Messages.JoinMessage;

public class VirtualSynchrony {
    private final static int PARTICIPANTS = 3;

    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create("virtual-synchrony");

        ActorRef groupManager = system.actorOf(GroupManager.props(), "group-manager");

        // initial ID for group participants
        int id = 1;

        List<ActorRef> initialGroup = new ArrayList<>();
        for (int i = 0; i < PARTICIPANTS; i++) {
            initialGroup.add(system.actorOf(
                Participant.props(id),
                "participant-"+ i)
            );
            id++;
        }
        initialGroup = Collections.unmodifiableList(initialGroup);

        for (ActorRef member : initialGroup) {
            groupManager.tell(new JoinMessage(), member);
        }

        try {
            System.out.println(">>> Press ENTER to exit <<<");
            System.in.read();
            // TODO: define a menu

        }
        catch (IOException ioe) {
            system.terminate();
        }
    }
}
