package com.staniul.security.clientaccesscheck;

import com.staniul.teamspeak.query.Client;

import java.util.Set;

/**
 * Checks if client channelgroup is one of the given groups.
 */
public class ClientChannelgroupAccessCheck extends ClientGroupAccessCheck {
    public ClientChannelgroupAccessCheck(Set<Integer> groups) {
        super(groups);
    }

    @Override
    public Boolean apply(Client client) {
        return groups.contains(client.getChannelgroup());
    }
}
