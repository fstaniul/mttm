package com.staniul.api.security;

import com.staniul.api.security.auth.ApiClientDetails;
import com.staniul.api.security.auth.JwtAuthenticationRequest;
import com.staniul.api.security.auth.Scope;
import com.staniul.api.security.auth.JwtAuthenticationResponse;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/auth")
@UseConfig("api/secure/scopes.xml")
public class AuthController {
    private static Logger log = LogManager.getLogger(AuthController.class);

    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;
    private final JwtTokenUtil tokenUtil;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthController(Query query, JwtTokenUtil tokenUtil, AuthenticationManager authenticationManager) {
        this.query = query;
        this.tokenUtil = tokenUtil;
        this.authenticationManager = authenticationManager;
    }

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> authenticateUser(@RequestBody JwtAuthenticationRequest details) {
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

                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(apiClientDetails, null, apiClientDetails.getGrantedAuthorities())
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);

                String token = tokenUtil.generateAuthenticationToken(apiClientDetails);

                return new ResponseEntity<>(new JwtAuthenticationResponse(token), HttpStatus.OK);
            }
        } catch (QueryException e) {
            log.error("Failed to get client list from server, thus authentication of user failed.");
        }

        return new ResponseEntity<>("Failed to authorize client!", HttpStatus.UNAUTHORIZED);
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
