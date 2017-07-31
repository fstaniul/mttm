package com.staniul.teamspeak.modules.channelsmanagers.vipchannels;

import org.springframework.jdbc.core.RowMapper;

public class VipChannel {
    public static RowMapper<VipChannel> rowMapper() {
        return (rs, i) -> new VipChannel(rs.getInt("number"), rs.getInt("id"), rs.getInt("owner"));
    }

    private int number;
    private int id;
    private int owner;

    public VipChannel(int number, int id, int owner) {
        this.number = number;
        this.id = id;
        this.owner = owner;
    }

    public int getNumber() {
        return number;
    }

    public int getId() {
        return id;
    }

    public int getOwner() {
        return owner;
    }
}