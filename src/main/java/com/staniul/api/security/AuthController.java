package com.staniul.api.security;

import com.staniul.api.security.auth.ApiClientDetails;
import com.staniul.api.security.auth.AuthenticationPostDetails;
import com.staniul.api.security.auth.Scope;
import com.staniul.api.security.auth.TokenHolder;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/auth")
@UseConfig("api/scopes.xml")
public class AuthController {
    private static Logger log = LogManager.getLogger(AuthController.class);

    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;
    private final JwtTokenUtil tokenUtil;

    @Autowired
    public AuthController(Query query, JwtTokenUtil tokenUtil) {
        this.query = query;
        this.tokenUtil = tokenUtil;
    }

    @RequestMapping(method = RequestMethod.POST)
    public @ResponseBody
    TokenHolder authenticateUser(@RequestBody AuthenticationPostDetails details) {
        try {
            Client clientsOnline = query.getClientList().stream()
                    .filter(c -> c.getDatabaseId() == details.getId())
                    .findFirst()
                    .orElse(null);

            if (clientsOnline != null &&
                    clientsOnline.getIp().equals(details.getIp()) &&
                    clientsOnline.getUniqueId().equals(details.getUniqueId())) {

                List<String> clientScopes = getClientScopes(clientsOnline);

                ApiClientDetails apiClientDetails = new ApiClientDetails(clientsOnline.getDatabaseId(), clientScopes);

                String token = tokenUtil.generateAuthenticationToken(apiClientDetails);

                return new TokenHolder(token);
            }
        } catch (QueryException e) {
            log.error("Failed to get client list from server, thus authentication of user failed.");
        }

        return new TokenHolder("");
    }

    private List<String> getClientScopes(Client clientsOnline) {
        List<String> clientScopes = new ArrayList<>();
        List<Scope> scopes = config.getClasses(Scope.class, "scope");
        for (Scope scope : scopes) {
            if (clientsOnline.isInServergroup(scope.getGroups()))
                clientScopes.add(scope.getScope());
        }

        return clientScopes;
    }
}
