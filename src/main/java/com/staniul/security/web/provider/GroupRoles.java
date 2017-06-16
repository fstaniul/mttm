package com.staniul.security.web.provider;

import com.staniul.xmlconfig.annotations.ConfigField;

import java.util.Set;

public class GroupRoles {
    @ConfigField(value = "ids", converter = StringToIntegerSetConverter.class)
    private Set<Integer> groupIds;

    @ConfigField(value = "roles", converter = StringToRolesSetConverter.class)
    private Set<String> roles;

    public GroupRoles() {
    }

    public GroupRoles(Set<Integer> groupIds, Set<String> roles) {
        this.groupIds = groupIds;
        this.roles = roles;
    }

    public Set<Integer> getGroupIds() {
        return groupIds;
    }

    public Set<String> getRoles() {
        return roles;
    }

    @Override
    public String toString() {
        return String.format("Ids: %s | Roles: %s", groupIds, roles);
    }
}
