package it.unitn.ds1.project1718;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import it.unitn.ds1.project1718.Messages.CrashMessage;
import it.unitn.ds1.project1718.Messages.JoinMessage;
import it.unitn.ds1.project1718.Messages.StartMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class VirtualSynchrony {
    private final static int PARTICIPANTS = 2;

    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create("virtual-synchrony");

        ActorRef groupManager = system.actorOf(GroupManager.props(), "0");

        // initial ID for group currentView
        int id = 1;
        List<ActorRef> actorsGroup = new ArrayList<>();
        for (; id <= PARTICIPANTS; id++) {
            actorsGroup.add(system.actorOf(
                Participant.props(),
                String.valueOf("node-" + id))
            );
        }

        // insert the group manager in the initial view
        groupManager.tell(new StartMessage(groupManager), ActorRef.noSender());

        for (ActorRef member : actorsGroup) {
            groupManager.tell(new JoinMessage(), member);
        }

        try {
            String command = "";
            Scanner scanner = new Scanner(System.in);
            System.out.println("Commands:\n"
                + "\te   -> exit\n"
                + "\tj   -> create new node and join to the group\n"
                + "\tc x -> set node x in a crashed state\n"
            );
            while (!command.equals("e")) {
                System.out.println("Insert next command:");
                command = scanner.next();
                switch (command) {
                    case "j" :
                        ActorRef newActor = system.actorOf(Participant.props(), String.valueOf("node-" + id++));
                        actorsGroup.add(newActor);
                        groupManager.tell(new JoinMessage(), newActor);
                        break;
                    case "c" :
                        int node = scanner.nextInt();
                        if (node < actorsGroup.size()) {
                            ActorRef toCrash = actorsGroup.get(node);
                            toCrash.tell(new CrashMessage(), ActorRef.noSender());
                        }
                        else {
                            System.out.println("Given actor doesn't exist or has already crashed!");
                        }
                        break;
                }
            }
            scanner.close();
        }
        catch (NoSuchElementException nsee) {}
        system.terminate();
    }
}
