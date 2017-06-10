package com.staniul.core.security.clientaccesscheck;

import com.staniul.query.Client;
import com.staniul.util.SetUtil;

import java.util.Set;

public class ClientChannelgroupAccessCheck implements ClientAccessCheck {
    private Set<Integer> groups;

    public ClientChannelgroupAccessCheck(Integer... groups) {
        this(SetUtil.arrayAsSet(groups));
    }

    public ClientChannelgroupAccessCheck(Set<Integer> groups) {
        this.groups = groups;
    }

    @Override
    public Boolean apply(Client client) {
        return groups.contains(client.getChannelgroup());
    }
}
