package com.staniul.teamspeak.modules.channelsmanagers;

import org.springframework.jdbc.core.RowMapper;

import java.io.Serializable;

public class PrivateChannel implements Serializable {
    public transient static final int FREE_CHANNEL_OWNER = -1;

    public static RowMapper<PrivateChannel> rowMapper () {
        return (rs, rowNum) -> new PrivateChannel(rs.getInt("channel_id"), rs.getInt("number"), rs.getInt("owner_id"));
    }

    private int id;
    private int number;
    private int owner;

    public PrivateChannel(int number) {
        this.number = number;
    }

    public PrivateChannel(int id, int number, int owner) {
        this.id = id;
        this.number = number;
        this.owner = owner;
    }

    public int getId() {
        return id;
    }

    public int getNumber() {
        return number;
    }

    public int getOwner() {
        return owner;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setOwner(int owner) {
        this.owner = owner;
    }

    public boolean isFree () {
        return owner == FREE_CHANNEL_OWNER;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    @Override
    public String toString() {
        return String.format("(%d: %d %d)", number, id, owner);
    }
}
