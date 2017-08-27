package com.staniul.teamspeak.modules.channelsmanagers;

import java.util.List;

public class VipChannel {
    private int channelId;
    private int ownerId;
    private int spacerId;
    private List<Integer> subChannelsIds;

    public VipChannel(int channelId, int ownerId, int spacerId) {
        this.channelId = channelId;
        this.ownerId = ownerId;
        this.spacerId = spacerId;
    }

    public int getChannelId() {
        return channelId;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public int getSpacerId() {
        return spacerId;
    }

    public List<Integer> getSubChannelsIds() {
        return subChannelsIds;
    }

    public void setSubChannelsIds(List<Integer> subChannelsIds) {
        this.subChannelsIds = subChannelsIds;
    }
}
