package com.staniul.security.clientaccesscheck;

import com.staniul.teamspeak.query.Client;

import java.util.Set;

/**
 * Permits clients whos channelgroup is other then one given.
 */
public class ClientNotChannelgroupAccessCheck extends ClientGroupAccessCheck {
    public ClientNotChannelgroupAccessCheck(Set<Integer> groups) {
        super(groups);
    }

    @Override
    public Boolean apply(Client client) {
        return !groups.contains(client.getChannelgroup());
    }
}
