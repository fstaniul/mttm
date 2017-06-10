package com.staniul.teamspeakcore.security.clientaccesscheck;

import com.staniul.teamspeakcore.security.AccessCheck;
import com.staniul.query.Client;
import com.staniul.util.SetUtil;

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
