package com.staniul.teamspeak.torunament.data;

import org.springframework.jdbc.core.RowMapper;

public class TournamentPlayer {
    private String name;
    private String nickname;
    private String position;

    public TournamentPlayer(String name, String nickname, String position) {
        this.name = name;
        this.nickname = nickname;
        this.position = position;
    }

    public String getPosition() {
        return position;
    }

    public String getName() {
        return name;
    }

    public String getNickname() {
        return nickname;
    }

    public static RowMapper<TournamentPlayer> rowMapper() {
        return (rs, i) -> new TournamentPlayer(rs.getString("name"), rs.getString("nickname"), rs.getString("position"));
    }
}
