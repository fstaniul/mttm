package com.staniul.teamspeakcore.security.clientaccesscheck;

import com.staniul.teamspeakcore.security.AccessCheck;
import com.staniul.query.Client;
import com.staniul.util.SetUtil;

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
