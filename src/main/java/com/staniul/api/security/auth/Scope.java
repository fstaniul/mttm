package com.staniul.api.security.auth;

import com.staniul.util.json.Otjs;
import com.staniul.xmlconfig.annotations.ConfigField;
import com.staniul.xmlconfig.convert.StringToIntegerSetConverter;

import java.util.Set;

public class Scope {
    @ConfigField(value = "groups", converter = StringToIntegerSetConverter.class)
    private Set<Integer> groups;

    @ConfigField(value = "role")
    private String scope;

    public Scope() {
    }

    public Scope(Set<Integer> groups, String scope) {
        this.groups = groups;
        this.scope = scope;
    }

    public Set<Integer> getGroups() {
        return groups;
    }

    public String getScope() {
        return scope;
    }

    @Override
    public String toString() {
        return Otjs.format(this);
    }
}
