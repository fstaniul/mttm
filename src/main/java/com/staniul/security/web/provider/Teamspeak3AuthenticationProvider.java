package com.staniul.security.web.provider;

import com.staniul.security.web.Teamspeak3AuthenticationToken;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@UseConfig("secure/auth-provider.xml")
public class Teamspeak3AuthenticationProvider implements AuthenticationProvider {
    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;

    @Autowired
    public Teamspeak3AuthenticationProvider(Query query) {
        this.query = query;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Teamspeak3AuthenticationToken auth = (Teamspeak3AuthenticationToken) authentication;
        int databaseId = auth.getDatabaseId();
        String lastIp = auth.getIp();

        try {
            Client client = query.getClientList().stream().filter(c -> c.getDatabaseId() == databaseId && c.getIp().equals(lastIp)).findFirst().orElse(null);
            if (client == null) return authentication;

            List<GrantedAuthority> grantedAuthorities = getAuthorities(client);
            grantedAuthorities.addAll(auth.getAuthorities());

            return new Teamspeak3AuthenticationToken(auth.getDatabaseId(), auth.getIp(), grantedAuthorities);
        } catch (QueryException e) {
            return authentication;
        }
    }

    private List<GrantedAuthority> getAuthorities(Client client) {
        List<GroupRoles> roles = config.getClasses(GroupRoles.class, "groups");
        for (GroupRoles role : roles) {
            if (client.isInServergroup(role.getGroupIds()))
                return role.getRoles().stream().map(Teamspeak3GrantedAuthority::new).collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return aClass.equals(Teamspeak3AuthenticationToken.class);
    }
}
