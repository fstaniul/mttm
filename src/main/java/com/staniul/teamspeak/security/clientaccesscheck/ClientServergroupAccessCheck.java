package com.staniul.teamspeak.security.clientaccesscheck;

import com.staniul.query.Client;

import java.util.Set;

public class ClientServergroupAccessCheck extends ClientGroupAccessCheck {
    public ClientServergroupAccessCheck(Set<Integer> groups) {
        super(groups);
    }

    @Override
    public Boolean apply(Client client) {
        return client.isInServergroup(groups);
    }
}
