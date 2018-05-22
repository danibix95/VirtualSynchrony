package it.unitn.ds1.project1718;

import akka.actor.ActorSystem;

import java.io.IOException;

public class VirtualSynchrony {
    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create("virtual-synchrony");

        try {
            System.out.println(">>> Press ENTER to exit <<<");
            System.in.read();
        }
        catch (IOException ioe) {
            system.terminate();
        }
    }
}
