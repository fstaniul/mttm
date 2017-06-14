package com.staniul.security.clientaccesscheck;

import com.staniul.teamspeak.query.Client;

import java.util.Set;

/**
 * Checks if client is in one of many a servergroups based on given groups.
 */
public class ClientServergroupAccessCheck extends ClientGroupAccessCheck {
    public ClientServergroupAccessCheck(Set<Integer> groups) {
        super(groups);
    }

    @Override
    public Boolean apply(Client client) {
        return client.isInServergroup(groups);
    }
}
