package com.staniul.modules.messengers;

import com.staniul.security.web.provider.StringToIntegerSetConverter;
import com.staniul.teamspeak.query.Client;
import com.staniul.xmlconfig.annotations.ConfigField;

import java.util.Set;

public class WelcomeMessage {
    @ConfigField(value = "groups", converter = StringToIntegerSetConverter.class)
    private Set<Integer> groups;
    @ConfigField("msg")
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
