package com.staniul.teamspeak.query;

import com.staniul.teamspeak.query.client.Platform;
import com.staniul.teamspeak.query.client.Voice;
import com.staniul.util.collections.SetUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contains all information about client.
 */
public class Client {
    private int id;
    private int databaseId;
    private String uniqueId;
    private String hash64UniqueId;
    private int currentChannelId;

    private String nickname;
    private String ip;

    private Set<Integer> servergroups;
    private int channelgroup;

    private boolean query;
    private boolean commander;
    private boolean away;

    private Platform platform;

    private long timeCreated;
    private long timeLastConnected;

    private Voice microphone;
    private Voice headphones;

    public Client (Client client) {
        id = client.getId();
        databaseId = client.getDatabaseId();
        uniqueId = client.getUniqueId();
        hash64UniqueId = client.getHash64UniqueId();
        currentChannelId = client.getCurrentChannelId();

        nickname = client.getNickname();
        ip = client.getIp();

        servergroups = client.getServergroups();
        channelgroup = client.getChannelgroup();

        query = client.isQuery();
        commander = client.isCommander();
        away = client.isAway();

        platform = client.getPlatform();

        microphone = client.getMicrophone();
        headphones = client.getHeadphones();
    }

    public Client(int clientId, Map<String, String> info) {
        id = clientId;
        databaseId = Integer.parseInt(info.get("client_database_id"));
        uniqueId = info.get("client_unique_identifier");
        hash64UniqueId = info.get("client_base64HashClientUID");
        currentChannelId = Integer.parseInt(info.get("cid"));

        nickname = info.get("client_nickname");
        ip = info.get("connection_client_ip");

        servergroups = Collections.unmodifiableSet(
                Arrays.stream(info.get("client_servergroups").split(","))
                        .map(Integer::parseInt)
                        .collect(Collectors.toSet())
        );

        channelgroup = Integer.parseInt(info.get("client_channel_group_id"));

        query = info.get("client_type").equals("1");
        commander = info.get("client_is_channel_commander").equals("1");
        away = info.get("client_away").equals("1");

        platform = Platform.parse(info.get("client_platform"));

        timeCreated = Long.parseLong(info.get("client_created"));
        timeLastConnected = Long.parseLong(info.get("client_lastconnected"));

        microphone = new Voice(info.get("client_input_hardware").equals("1"),
                info.get("client_input_muted").equals("1"));

        headphones = new Voice(info.get("client_output_hardware").equals("1"),
                info.get("client_output_muted").equals("1"));
    }

    public int getId() {
        return id;
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

    public int getCurrentChannelId() {
        return currentChannelId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getIp() {
        return ip;
    }

    public boolean isInServergroup (int group) {
        return servergroups.contains(group);
    }

    public boolean isInServergroup (Set<Integer> groups) {
        return SetUtil.countIntersection(servergroups, groups) > 0L;
    }

    public boolean isOnlyInServergroup (int group) {
        return servergroups.size() == 1 && servergroups.contains(group);
    }

    public Set<Integer> getServergroups() {
        return servergroups;
    }

    public int getChannelgroup() {
        return channelgroup;
    }

    public boolean isQuery() {
        return query;
    }

    public boolean isCommander() {
        return commander;
    }

    public boolean isAway() {
        return away;
    }

    public Platform getPlatform() {
        return platform;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public long getTimeLastConnected() {
        return timeLastConnected;
    }

    public long getTimeConnected() {
        return System.currentTimeMillis() / 1000 - timeLastConnected;
    }

    public Voice getMicrophone() {
        return microphone;
    }

    public Voice getHeadphones() {
        return headphones;
    }

    @Override
    public String toString() {
        return String.format("%d / %d / %s", id, databaseId, nickname);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Client &&
                ((Client) obj).databaseId == databaseId;
    }
}
