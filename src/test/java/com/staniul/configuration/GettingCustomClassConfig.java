package com.staniul.configuration;

import com.staniul.xmlconfig.ConfigFile;
import com.staniul.xmlconfig.ConfigurationLoader;
import com.staniul.teamspeak.Group;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

@ConfigFile("groups.xml")
public class GettingCustomClassConfig {
    private CustomXMLConfiguration config;

    @Test
    public void testGetGroupClass () throws Exception {
        config = ConfigurationLoader.load(GettingCustomClassConfig.class);
        List<Group> groups = config.getClasses(Group.class, "headAdministrators");
        System.out.println(groups);
//        System.out.println(config.getString("headAdministrators.group[@id]"));
    }
}
