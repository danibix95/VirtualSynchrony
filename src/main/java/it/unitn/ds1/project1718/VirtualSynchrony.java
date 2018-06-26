package it.unitn.ds1.project1718;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import it.unitn.ds1.project1718.Messages.CrashMessage;
import it.unitn.ds1.project1718.Messages.CrashWhileSendingMessage;
import it.unitn.ds1.project1718.Messages.CrashAfterReceiveMessage;
import it.unitn.ds1.project1718.Messages.CrashOnViewChangeMessage;
import it.unitn.ds1.project1718.Messages.JoinMessage;
import it.unitn.ds1.project1718.Messages.StartMessage;

import java.io.File;
import java.util.*;

public class VirtualSynchrony {
    private final static int PARTICIPANTS = 3;

    public static void main(String[] args) {
        // create logs folder
        try {
            new File("vs-logs").mkdir();
        }
        catch (SecurityException se) {
            System.err.println("Impossible to create folder in which logs will be saved!");
            se.printStackTrace();
        }
        final ActorSystem system = ActorSystem.create("virtual-synchrony");

        ActorRef groupManager = system.actorOf(GroupManager.props(), "0");

        // initial ID for group currentView
        int id = 1;
        HashMap<Integer, ActorRef> actorsGroup = new HashMap<>();
        for (; id <= PARTICIPANTS; id++) {
            actorsGroup.put(id, system.actorOf(
                Participant.props(),
                String.valueOf("node-" + id))
            );
        }

        // insert the group manager in the initial view
        groupManager.tell(new StartMessage(groupManager), ActorRef.noSender());

        actorsGroup.forEach((k, actor) -> groupManager.tell(new JoinMessage(), actor));

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
                    case "j":
                        ActorRef newActor =
                            system.actorOf(Participant.props(), String.valueOf("node-" + id));
                        actorsGroup.putIfAbsent(id, newActor);
                        groupManager.tell(new JoinMessage(), newActor);
                        System.out.println("New actor (" + id  + ") joined!");

                        id++;
                        break;
                    case "c":
                        manageCrashMessage(scanner, actorsGroup, new CrashMessage());
                        break;
                    case "cs":
                        manageCrashMessage(scanner, actorsGroup, new CrashWhileSendingMessage());
                        break;
                    case "cr":
                        manageCrashMessage(scanner, actorsGroup, new CrashAfterReceiveMessage());
                        break;
                    case "cv":
                        manageCrashMessage(scanner, actorsGroup, new CrashOnViewChangeMessage());
                        break;
                    case "e":
                        System.out.println("Exit...");
                        break;
                    default:
                        System.out.println("Command not recognized! Try again");
                }
            }
            scanner.close();
        }
        catch (IllegalStateException ise) {}
        system.terminate();
    }

    private static void manageCrashMessage(Scanner scr, HashMap<Integer, ActorRef> actors, CrashMessage msg) {
        try {
            int node = scr.nextInt()-1;
            ActorRef toCrash = actors.remove(node);
            if (toCrash != null) {
                toCrash.tell(msg, ActorRef.noSender());
                System.out.println("Selected actor informed to crash."
                    + " Crash will happen in 5 seconds!");
            }
            else {
                System.out.println("Given actor doesn't exist!");
            }
        }
        catch (InputMismatchException ime) {
            System.out.println("\nYou've not passed an integer id!");
            scr.skip(".*");
        }
        catch (NoSuchElementException nsee) {
            System.out.println("\nNo input passed!");
        }
    }
}
