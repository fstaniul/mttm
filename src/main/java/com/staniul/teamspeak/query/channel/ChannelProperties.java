package com.staniul.teamspeak.query.channel;

import de.stefan1200.jts3serverquery.JTS3ServerQuery;

/**
 * Properties of a channel. Used to create new channels.
 */
public class ChannelProperties implements ChannelFlagConstants {
    private String name;
    private String topic;
    private String description;

    private int maxClients;
    private int maxFamilyClients;

    private int parent;
    private int order;

    private int codec;
    private int codecQuality;

    private int flags;

    public ChannelProperties () {
        name = "";
        topic = "";
        description = "";
        maxClients = -1;
        maxFamilyClients = -1;
        parent = -1;
        order = -1;
        codec = 4;
        codecQuality = 10;
        flags = __DEFAULT_FLAGS;
    }

    public String getName() {
        return name.length() > 40 ? name.substring(0, 40) : name;
    }

    public ChannelProperties setName(String name) {
        this.name = name;
        return this;
    }

    public String getTopic() {
        return topic;
    }

    public ChannelProperties setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ChannelProperties setDescription(String description) {
        this.description = description;
        return this;
    }

    public int getMaxClients() {
        return maxClients;
    }

    public ChannelProperties setMaxClients(int maxClients) {
            this.maxClients = maxClients;
        return this;
    }

    public int getMaxFamilyClients() {
        return maxFamilyClients;
    }

    public ChannelProperties setMaxFamilyClients(int maxFamilyClients) {
        this.maxFamilyClients = maxFamilyClients;
        return this;
    }

    public int getParent() {
        return parent;
    }

    public ChannelProperties setParent(int parent) {
        this.parent = parent;
        return this;
    }

    public int getOrder() {
        return order;
    }

    public ChannelProperties setOrder(int order) {
        this.order = order;
        return this;
    }

    public int getCodec() {
        return codec;
    }

    public ChannelProperties setCodec(int codec) {
        this.codec = codec;
        return this;
    }

    public int getCodecQuality() {
        return codecQuality;
    }

    public ChannelProperties setCodecQuality(int codecQuality) {
        this.codecQuality = codecQuality;
        return this;
    }

    public int getFlags() {
        return flags;
    }

    public ChannelProperties setFlag (int flag) {
        flags ^= flag;
        return this;
    }

    public String toTeamspeak3QueryString () {
        StringBuilder sb = new StringBuilder();

        sb.append("channel_name=").append(JTS3ServerQuery.encodeTS3String(getName())).append(" ");
        if (!"".equals(description))
            sb.append("channel_description=").append(JTS3ServerQuery.encodeTS3String(description)).append(" ");
        if (!"".equals(topic)) sb.append("channel_topic=").append(JTS3ServerQuery.encodeTS3String(topic)).append(" ");
        if (maxClients >= 0) sb.append("channel_maxclients=").append(maxClients).append(" ");
        if (maxFamilyClients >= 0) sb.append("channel_maxfamilyclients=").append(maxFamilyClients).append(" ");
        if (parent > 0) sb.append("cpid=").append(parent).append(" ");
        if (order > 0) sb.append("channel_order=").append(order).append(" ");
        if (codec > 0) sb.append("channel_codec=").append(codec).append(" ");
        if (codecQuality > 0) sb.append("channel_codec_quality=").append(codecQuality).append(" ");
        sb.append(ChannelFlagConstants.flagsToString(flags));

        return sb.toString();
    }

    @Override
    public String toString() {
        return "Name: " + getName() + ", " +
                "Topic: " + getTopic() + ", " +
                "Parent: " + getParent() + ", " +
                "Order: " + getOrder();
    }
}
