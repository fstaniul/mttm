package com.staniul.modules.messengers;

import com.staniul.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.commands.validators.ValidateParams;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@UseConfig("modules/cm.xml")
public class ClientMessenger {
    private static Logger log = Logger.getLogger(ClientMessenger.class);

    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;

    @Autowired
    public ClientMessenger(Query query) {
        this.query = query;
    }

    @Teamspeak3Command("!msg")
    @ClientGroupAccess("servergroups.admins")
    @ValidateParams(NotEmptyParamsValidator.class)
    public CommandResponse messageClients (Client client, String params) throws QueryException {
        List<Client> clients = query.getClientList().stream()
                .filter(c -> c.isInServergroup(config.getIntSet("groups.clients[@ids]")))
                .collect(Collectors.toList());

        sendMessageToClients(clients, params);

        return new CommandResponse(config.getString("responses.msg[@msg]"));
    }

    @Teamspeak3Command("!msgall")
    @ClientGroupAccess("servergroups.admins")
    @ValidateParams(NotEmptyParamsValidator.class)
    public CommandResponse messageAllClients (Client client, String params) throws QueryException {
        List<Client> clients = query.getClientList().stream()
                .filter(c -> !c.equals(client))
                .collect(Collectors.toList());

        sendMessageToClients(clients, params);

        return new CommandResponse(config.getString("responses.msgall[@msg]"));
    }

    @Teamspeak3Command("!msgadmin")
    @ClientGroupAccess("servergroups.admins")
    @ValidateParams(NotEmptyParamsValidator.class)
    public CommandResponse messageAllAdmins (Client client, String params) throws QueryException {
        List<Client> clients = query.getClientList().stream()
                .filter(c -> c.isInServergroup(config.getIntSet("groups.admins[@ids]")))
                .filter(c -> !c.equals(client))
                .collect(Collectors.toList());

        sendMessageToClients(clients, params);

        return new CommandResponse(config.getString("responses.msgadmin[@msg]"));
    }

    private void sendMessageToClients (List<Client> clients, String msg) {
        for (Client c : clients) {
            try {
                query.sendTextMessageToClient(c.getId(), msg);
            } catch (QueryException e) {
                log.error(String.format("Failed to send message to client (%d %s) with message (%s).", c.getDatabaseId(), c.getNickname(), msg), e);
            }
        }
    }
}
