package com.staniul.teamspeak.commands.core;

import com.staniul.xmlconfig.convert.StringToIntegerSetConverter;
import com.staniul.xmlconfig.annotations.ConfigField;

import java.util.Set;

public class Scope {
    private int id;
    @ConfigField(value = "groups", converter = StringToIntegerSetConverter.class)
    private Set<Integer> groups;

    public Scope() {
    }

    public Scope(int id, Set<Integer> groups) {
        this.id = id;
        this.groups = groups;
    }

    public int getId() {
        return id;
    }

    public Set<Integer> getGroups() {
        return groups;
    }
}
