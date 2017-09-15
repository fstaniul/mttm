package com.staniul.teamspeak.torunament.data;

import org.springframework.jdbc.core.RowMapper;

import java.util.List;

public class TournamentTeam {
    public static RowMapper<TournamentTeam> rowMapper() {
        return (rs, i) -> new TournamentTeam(rs.getString("name"), rs.getInt("id"));
    }

    private String name;
    private int id;
    private List<TournamentPlayer> players;

    public TournamentTeam(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public List<TournamentPlayer> getPlayers() {
        return players;
    }

    public void setPlayers(List<TournamentPlayer> players) {
        this.players = players;
    }
}
