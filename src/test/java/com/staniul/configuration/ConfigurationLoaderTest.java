package com.staniul.configuration;

import com.staniul.modules.adminlists.AdminOnlineList;
import com.staniul.modules.adminlists.Servergroup;
import com.staniul.modules.registerers.ClientRegisterer;
import com.staniul.teamspeak.query.Query;
import com.staniul.xmlconfig.ConfigurationLoader;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ConfigurationLoaderTest {
    @Test
    public void loadConfiguration() throws Exception {
        Configuration configuration = ConfigurationLoader.load(Query.class);
        assertNotNull(configuration);
        assertEquals("localhost", configuration.getString("ip"));
    }

    @Test
    public void loadConfigTestComma () throws Exception {
        CustomXMLConfiguration configuration = ConfigurationLoader.load(ClientRegisterer.class);
        System.out.println(configuration.getIntSet("groups.admins[@id]"));
    }

//    @Test
    public void testConfigurationLaod () throws Exception {
        CustomXMLConfiguration configuration = ConfigurationLoader.load(AdminOnlineList.class);
        System.out.println(configuration.getString("groups.servergroup[@icon]"));
        List<Servergroup> servergroups = configuration.getClasses(Servergroup.class, "groups.servergroup");
        System.out.println(servergroups);
    }
}