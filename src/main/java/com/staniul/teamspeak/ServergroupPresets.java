package com.staniul.teamspeak;

import com.staniul.util.SetUtil;
import com.staniul.xmlconfig.ConfigurationLoader;
import com.staniul.xmlconfig.ConfigFile;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

@Component
@ConfigFile("groups.xml")
public class ServergroupPresets {
    private Set<Group> administrators;
    private Set<Group> headAdministrators;
    private Set<Group> ignored;
    private Set<Group> registered;
    private Set<Group> guest;

    public ServergroupPresets () throws ConfigurationException {
        CustomXMLConfiguration config = ConfigurationLoader.load(ServergroupPresets.class);
        initGroups(config);
    }

    private void initGroups(CustomXMLConfiguration config) {
        administrators = SetUtil.form(config.getClasses(Group.class, "administrators"));
        headAdministrators = SetUtil.form(config.getClasses(Group.class, "headAdministrators"));
        ignored = SetUtil.form(config.getClasses(Group.class, "ignored"));
        registered = SetUtil.form(config.getClasses(Group.class, "registered"));
        guest = SetUtil.form(config.getClasses(Group.class, "guest"));
    }

    public Set<Group> getAdministrators() {
        return Collections.unmodifiableSet(administrators);
    }

    public Set<Group> getHeadAdministrators() {
        return Collections.unmodifiableSet(headAdministrators);
    }

    public Set<Group> getIgnored() {
        return Collections.unmodifiableSet(ignored);
    }

    public Set<Group> getRegistered() {
        return Collections.unmodifiableSet(registered);
    }

    public Set<Group> getGuest() {
        return Collections.unmodifiableSet(guest);
    }
}
