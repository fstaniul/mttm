package com.staniul.teamspeak;

import com.staniul.xmlconfig.ConfigurationLoader;
import com.staniul.xmlconfig.ConfigFile;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.springframework.stereotype.Component;

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
        XMLConfiguration config = ConfigurationLoader.load(ServergroupPresets.class);

    }
}
