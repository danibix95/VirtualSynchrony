package it.unitn.ds1.project1718;

public class GroupManager extends Node {
    private final int MULTICAST_TIMEOUT = 5000;

    @Override
    public Receive createReceive() {
        return null;
    }

    public GroupManager() {
        super(0);
    }
}
