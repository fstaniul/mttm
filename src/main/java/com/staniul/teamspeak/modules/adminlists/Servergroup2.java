package com.staniul.teamspeak.modules.adminlists;

public class Servergroup2 {
    private int id;
    private String name;
    private String icon;
    private boolean solo;

    public Servergroup2 () {}

    public Servergroup2(int id, String name, String icon) {
        this.id = id;
        this.name = name;
        this.icon = icon;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public boolean isSolo() {
        return solo;
    }
}
