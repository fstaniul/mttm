package com.staniul.teamspeak.query;

import com.staniul.teamspeak.query.channel.ChannelFlagConstants;

import java.util.Map;

/**
 * Contains information about teamspeak 3 channel
 */
public class Channel implements ChannelFlagConstants {
    private int id;
    private int parentId;
    private int orderId;

    private String name;
    private String topic;

    private int codec;
    private int codecQuality;

    private long secondsEmpty;

    private int totalClients; //Supported only on channel list
    private int totalFamilyClients;

    private int flags;

    Channel(int id, Map<String, String> info) {
        this.id = id;
        parentId = Integer.parseInt(info.get("pid"));
        orderId = Integer.parseInt(info.get("channel_order"));

        name = info.get("channel_name");
        topic = info.get("channel_topic");

        codec = Integer.parseInt(info.get("channel_codec"));
        codecQuality = Integer.parseInt(info.get("channel_codec_quality"));

        secondsEmpty = Long.parseLong(info.get("seconds_empty"));

        if (info.get("total_clients") != null) {
            totalClients = Integer.parseInt(info.get("total_clients"));
            totalFamilyClients = Integer.parseInt(info.get("total_clients_family"));
        }

        flags = ChannelFlagConstants.parseFlags(info);
    }

    public int getId() {
        return id;
    }

    public int getParentId() {
        return parentId;
    }

    public int getOrderId() {
        return orderId;
    }

    public String getName() {
        return name;
    }

    public String getTopic() {
        return topic;
    }

    public int getCodec() {
        return codec;
    }

    public int getCodecQuality() {
        return codecQuality;
    }

    public long getSecondsEmpty() {
        return secondsEmpty;
    }

    public int getTotalClients() {
        return totalClients;
    }

    public int getTotalFamilyClients() {
        return totalFamilyClients;
    }

    public boolean isPermanent() {
        return (flags & PERMANENT) == 1;
    }

    public boolean isSemiPermanent() {
        return (flags & SEMI_PERMANENT) == SEMI_PERMANENT;
    }

    public boolean areMaxClientsUnlimited() {
        return (flags & MAXCLIENTS_UNLIMITED) == MAXCLIENTS_UNLIMITED;
    }

    public boolean areFamilyMaxClientsUnlimited() {
        return (flags & MAXFAMILYCLIENTS_UNLIMITED) == MAXFAMILYCLIENTS_UNLIMITED;
    }

    public boolean isPasswordProtected() {
        return (flags & PASSWORD) == PASSWORD;
    }

    public boolean isDefaultChannel() {
        return (flags & DEFAULT) == DEFAULT;
    }

    /**
     * Sets a flag. If flag was already set it will disable the flag. It just simply does a XOR with flag.
     *
     * @param flag One of {@link ChannelFlagConstants}.
     */
    public void setFlag(int flag) {
        flags ^= flag;
    }
}
