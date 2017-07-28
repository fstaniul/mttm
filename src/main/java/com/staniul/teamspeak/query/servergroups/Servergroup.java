package com.staniul.teamspeak.query.servergroups;

import java.util.Map;

public class Servergroup {
    private int id;
    private String name;

    public Servergroup (Map<String, String> map) {
        id = Integer.parseInt(map.get("sgid"));
        name = map.get("name");
    }

    public Servergroup(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
