package com.staniul.teamspeak.query;

import java.util.Map;

public class ClientChannelInfo {
    private int channelId;
    private int clientDatabaseId;
    private int channelgroupId;

    public ClientChannelInfo(Map<String, String> map) {
        channelId = Integer.parseInt(map.get("cid"));
        clientDatabaseId = Integer.parseInt(map.get("cldbid"));
        channelgroupId = Integer.parseInt(map.get("cgid"));
    }

    public int getChannelId() {
        return channelId;
    }

    public int getClientDatabaseId() {
        return clientDatabaseId;
    }

    public int getChannelgroupId() {
        return channelgroupId;
    }
}
