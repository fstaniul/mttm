package com.staniul.query;

import com.staniul.query.channel.ChannelFlagConstants;

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

    private int flags;

    Channel(int id, Map<String, String> info) {
        this.id = id;
        parentId = Integer.parseInt(info.get("pid"));
        orderId = Integer.parseInt(info.get("channel_order"));

        name = info.get("channel_name");
        topic = info.get("channel_topic");

        codec = Integer.parseInt("channel_codec");
        codecQuality = Integer.parseInt("channel_codec_quality");

        secondsEmpty = Long.parseLong("seconds_empty");

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
    public void set(int flag) {
        flags ^= flag;
    }
}
