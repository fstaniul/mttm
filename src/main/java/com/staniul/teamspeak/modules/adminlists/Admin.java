package com.staniul.teamspeak.modules.adminlists;

import com.staniul.teamspeak.query.Client;

public class Admin extends Client {
    private int rank;
    private String icon;

    public Admin (Client client, int rank, String icon) {
        super(client);
        this.rank = rank;
        this.icon = icon;
    }

    public int getRank() {
        return rank;
    }

    public String getIcon() {
        return icon;
    }
}
