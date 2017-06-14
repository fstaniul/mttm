package com.staniul.security.clientaccesscheck;

import com.staniul.teamspeak.query.Client;
import com.staniul.util.SetUtil;

import java.util.Set;

/**
 * Checks if client groups are not the one given.
 */
public class ClientNotServergroupAccessCheck extends ClientGroupAccessCheck {
    public ClientNotServergroupAccessCheck(Set<Integer> groups) {
        super(groups);
    }

    @Override
    public Boolean apply(Client client) {
        return SetUtil.countIntersection(groups, client.getServergroups()) == 0L;
    }
}
