package com.staniul.core.security.clientaccesscheck;

import com.staniul.query.Client;
import com.staniul.util.SetUtil;

import java.util.Set;

public class ClientServergroupAccessCheck implements ClientAccessCheck {
    private Set<Integer> groups;

    public ClientServergroupAccessCheck(Integer... groups) {
        this(SetUtil.arrayAsSet(groups));
    }

    public ClientServergroupAccessCheck(Set<Integer> groups) {
        this.groups = groups;
    }

    @Override
    public Boolean apply(Client client) {
        return client.isInServergroup(groups);
    }
}
