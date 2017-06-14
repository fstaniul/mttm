package com.staniul.teamspeak.query;

import java.util.Map;

public class ClientDatabase {
    private int databaseId;
    private String uniqueId;
    private String hash64UniqueId;

    private String nickname;
    private String lastKnownIp;

    private long timeCreated;
    private long timeLastConnected;

    ClientDatabase (int databaseId, Map<String, String> info) {
        this.databaseId = databaseId;
        uniqueId = info.get("client_unique_identifier");
        hash64UniqueId = info.get("client_base64HashClientUID");

        nickname = info.get("client_nickname");
        lastKnownIp = info.get("client_lastip");

        timeCreated = Long.parseLong(info.get("client_created"));
        timeLastConnected = Long.parseLong(info.get("client_lastconnected"));
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getHash64UniqueId() {
        return hash64UniqueId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getLastKnownIp() {
        return lastKnownIp;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public long getTimeLastConnected() {
        return timeLastConnected;
    }
}
