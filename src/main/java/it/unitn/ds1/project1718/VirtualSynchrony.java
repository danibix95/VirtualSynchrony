package it.unitn.ds1.project1718;

import akka.actor.ActorSystem;

public class VirtualSynchrony {
    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create("virtual-synchrony");

        system.terminate();
    }
}
