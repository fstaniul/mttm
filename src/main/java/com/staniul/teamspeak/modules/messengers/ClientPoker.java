package com.staniul.teamspeak.modules.messengers;

import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.commands.validators.ValidateParams;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@UseConfig("modules/cp.xml")
public class ClientPoker {
    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;

    @Autowired
    public ClientPoker(Query query) {
        this.query = query;
    }

    private void internalPokeClients(Set<Integer> groups, String message) throws QueryException {
        List<Client> clients;
        if (groups == null) clients = query.getClientList();
        else clients = query.getClientList().stream()
                .filter(c -> c.isInServergroup(groups))
                .collect(Collectors.toList());

        for (Client client : clients)
            query.pokeClient(client.getId(), message, true);
    }

    private CommandResponse response () {
        return new CommandResponse(config.getString("response"));
    }

    @Teamspeak3Command("!pokeclients")
    @ClientGroupAccess("servergroups.admins")
    @ValidateParams(NotEmptyParamsValidator.class)
    public CommandResponse pokeClients(Client client, String params) throws QueryException {
        internalPokeClients(config.getIntSet("groups.clients"), params);
        return response();
    }

    @Teamspeak3Command("!pokeadmins")
    @ClientGroupAccess("servergroups.admins")
    @ValidateParams(NotEmptyParamsValidator.class)
    public CommandResponse pokeAdmins (Client client, String params) throws  QueryException {
        internalPokeClients(config.getIntSet("groups.admins"), params);
        return response();
    }

    @Teamspeak3Command("!poke")
    @ClientGroupAccess("servergroups.admins")
    @ValidateParams(NotEmptyParamsValidator.class)
    public CommandResponse pokeAll(Client client, String params) throws QueryException {
        internalPokeClients(null, params);
        return response();
    }
}
