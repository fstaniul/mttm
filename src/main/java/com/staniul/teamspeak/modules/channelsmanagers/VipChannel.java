package com.staniul.teamspeak.modules.channelsmanagers;

import org.springframework.jdbc.core.RowMapper;

import java.util.List;

public class VipChannel {
    public static RowMapper<VipChannel> rowMapper () {
        return (rs, rowNum) -> new VipChannel(rs.getInt("number"), rs.getInt("channel_id"), rs.getInt("owner_id"), rs.getInt("spacer_id"));
    }

    private int number;
    private int channelId;
    private int ownerId;
    private int spacerId;
    private List<Integer> subChannelsIds;

    public VipChannel(int number, int channelId, int ownerId, int spacerId) {
        this.number = number;
        this.channelId = channelId;
        this.ownerId = ownerId;
        this.spacerId = spacerId;
    }

    public int getNumber() {
        return number;
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

    @Override
    public String toString() {
        return String.format("VIP Channel %d with id %d and owner %d", number, channelId, ownerId);
    }
}
