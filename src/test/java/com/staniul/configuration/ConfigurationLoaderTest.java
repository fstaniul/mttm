package com.staniul.configuration;

import com.staniul.xmlconfig.ConfigurationLoader;
import com.staniul.query.Query;
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

}