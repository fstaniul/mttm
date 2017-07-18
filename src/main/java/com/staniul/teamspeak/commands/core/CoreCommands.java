package com.staniul.teamspeak.commands.core;

import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.query.Client;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@UseConfig("corecmd.xml")
public class CoreCommands {
    @WireConfig
    private CustomXMLConfiguration config;

    @Autowired
    public CoreCommands () {}

    @Teamspeak3Command("!help")
    @ClientGroupAccess("servergroups.admins")
    public CommandResponse listCommands (Client client, String params) {
        List<Command> commands = config.getClasses(Command.class, "command-list.cmd");
        List<Integer> clientScopes = getClientScopes (client);
        List<String> messages = commands.stream()
                .filter(c -> clientScopes.contains(c.getScope()))
                .map(c -> "[b]" + c.getCommand() + "[/b] - " + c.getDescription()).collect(Collectors.toList());
        return new CommandResponse(messages.toArray(new String[]{}));
    }

    private List<Integer> getClientScopes(Client client) {
        List<Scope> scopes = config.getClasses(Scope.class, "scopes.scope");
        List<Integer> clientScopes = new ArrayList<>();
        for (Scope scope : scopes) {
            if (client.isInServergroup(scope.getGroups()))
                clientScopes.add(scope.getId());
        }

        return clientScopes;
    }
}
