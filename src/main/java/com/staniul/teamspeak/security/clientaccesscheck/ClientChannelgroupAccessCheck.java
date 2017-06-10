package com.staniul.teamspeak.security.clientaccesscheck;

import com.staniul.query.Client;

import java.util.Set;

public class ClientChannelgroupAccessCheck extends ClientGroupAccessCheck {
    public ClientChannelgroupAccessCheck(Set<Integer> groups) {
        super(groups);
    }

    @Override
    public Boolean apply(Client client) {
        return groups.contains(client.getChannelgroup());
    }
}
