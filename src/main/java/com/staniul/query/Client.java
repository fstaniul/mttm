package com.staniul.query;

import com.staniul.query.client.Platform;
import com.staniul.query.client.Voice;
import com.staniul.util.SetsUtil;

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

    Client(int clientId, Map<String, String> info) {
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
        return SetsUtil.countIntersection(servergroups, groups) > 0L;
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
}
