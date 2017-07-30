package com.staniul.teamspeak.modules.channelsmanagers.privatechannels.dao;

public class PrivateChannel {
    private int number;
    private int id;
    private int owner;

    public PrivateChannel(int number, int id,int owner) {
        this.number = number;
        this.id = id;
        this.owner = owner;
    }

    public int getId() {
        return id;
    }

    public int getNumber() {
        return number;
    }

    public int getOwner() {
        return owner;
    }

    public boolean isFree() {
        return owner == -1;
    }
}
