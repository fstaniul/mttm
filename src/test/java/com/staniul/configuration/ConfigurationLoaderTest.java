package com.staniul.configuration;

import com.staniul.modules.registerer.ClientRegisterer;
import com.staniul.xmlconfig.ConfigurationLoader;
import com.staniul.teamspeak.query.Query;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.Test;

import static org.junit.Assert.*;

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

}