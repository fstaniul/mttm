package com.staniul.query.channel;

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

    }

    public String generateRequest() {
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMaxClients() {
        return maxClients;
    }

    public void setMaxClients(int maxClients) {
        this.maxClients = maxClients;
    }

    public int getMaxFamilyClients() {
        return maxFamilyClients;
    }

    public void setMaxFamilyClients(int maxFamilyClients) {
        this.maxFamilyClients = maxFamilyClients;
    }

    public int getParent() {
        return parent;
    }

    public void setParent(int parent) {
        this.parent = parent;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getCodec() {
        return codec;
    }

    public void setCodec(int codec) {
        this.codec = codec;
    }

    public int getCodecQuality() {
        return codecQuality;
    }

    public void setCodecQuality(int codecQuality) {
        this.codecQuality = codecQuality;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlag (int flag) {
        flags ^= flag;
    }
}
