package com.staniul.security.web.provider;

import com.staniul.xmlconfig.ConfigurationLoader;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import org.junit.Test;

import java.util.List;

public class GroupRolesTest {
    @Test
    public void testConfigLoading() throws Exception {
        CustomXMLConfiguration config = ConfigurationLoader.load(Teamspeak3AuthenticationProvider.class);
        List<GroupRoles> groupRoles = config.getClasses(GroupRoles.class, "groups");
        System.out.println(groupRoles);
    }
}