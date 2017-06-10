package com.staniul.teamspeakcore.security.clientaccesscheck;

import com.staniul.query.Client;
import com.staniul.teamspeakcore.security.AccessCheck;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class ClientGroupAccessCheck implements AccessCheck<Client> {
    protected Set<Integer> groups;

    public ClientGroupAccessCheck (Set<Integer> groups) {
        this.groups = groups;
    }
}
