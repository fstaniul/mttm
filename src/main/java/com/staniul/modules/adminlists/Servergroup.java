package com.staniul.modules.adminlists;

public class Servergroup {
    private int id;
    private int rank;
    private String icon;

    public Servergroup() {
    }

    public Servergroup(int id, int rank, String icon) {
        this.id = id;
        this.rank = rank;
        this.icon = icon;
    }

    public int getId() {
        return id;
    }

    public int getRank() {
        return rank;
    }

    public String getIcon() {
        return icon;
    }

    @Override
    public String toString() {
        return String.format("%d (%d): %s", id, rank, icon);
    }
}
