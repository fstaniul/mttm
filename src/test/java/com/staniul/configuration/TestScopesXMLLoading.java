package com.staniul.configuration;

import com.staniul.api.security.AuthController;
import com.staniul.api.security.auth.Scope;
import com.staniul.xmlconfig.ConfigurationLoader;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import org.junit.Test;

import java.util.List;

public class TestScopesXMLLoading {
    @Test
    public void test1 () throws Exception {
        CustomXMLConfiguration configuration = ConfigurationLoader.load(AuthController.class);
        List<Scope> scopes = configuration.getClasses(Scope.class, "scope");
        for (Scope scope : scopes) {
            System.out.println(scope);
        }
    }
}
