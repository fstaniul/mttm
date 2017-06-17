package com.staniul.modules.adminlists;

public class Servergroup {
    private int id;
    private int rank;
    private String icon;
    private String name;

    public Servergroup() {
    }

    public Servergroup(int id, int rank, String icon, String name) {
        this.id = id;
        this.rank = rank;
        this.icon = icon;
        this.name = name;
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

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("%d (%d): %s", id, rank, icon);
    }
}
