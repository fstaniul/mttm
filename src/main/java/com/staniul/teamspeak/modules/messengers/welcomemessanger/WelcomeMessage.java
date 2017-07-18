package com.staniul.teamspeak.modules.messengers.welcomemessanger;

import com.staniul.teamspeak.query.Client;

import java.util.Set;

public class WelcomeMessage {
    private Set<Integer> groups;
    private String message;

    public WelcomeMessage () {}

    public WelcomeMessage(Set<Integer> groups, String message) {
        this.groups = groups;
        this.message = message;
    }

    public Set<Integer> getGroups() {
        return groups;
    }

    public String getMessage() {
        return message;
    }

    public String getMessage (Client client) {
        return message.replace("$NICKNAME$", client.getNickname());
    }

    @Override
    public String toString() {
        return String.format("%s %s", getGroups(), getMessage());
    }
}
