package com.staniul.teamspeak.query.servergroups;

import java.util.Map;

public class ServergroupExtended extends Servergroup {
    private int sort;
    private int type;

    public ServergroupExtended(Map<String, String> map) {
        super(map);
        this.sort = Integer.parseInt(map.get("sortid"));
        this.type = Integer.parseInt(map.get("type"));
    }

    public ServergroupExtended(int id, String name, int sort, int type) {
        super(id, name);
        this.sort = sort;
        this.type = type;
    }

    public int getSort() {
        return sort;
    }

    public int getType() {
        return type;
    }
}
