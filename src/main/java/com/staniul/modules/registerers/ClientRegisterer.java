package com.staniul.modules.registerers;

import com.staniul.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.taskcontroller.Task;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@UseConfig("modules/reg.xml")
public class ClientRegisterer {
    private static Logger log = LogManager.getLogger(ClientRegisterer.class);

    @WireConfig
    private CustomXMLConfiguration config;
    private Query query;

    @Autowired
    public ClientRegisterer(Query query) {
        this.query = query;
    }

    @Task(delay = 1800000)
    public void checkForNewClients() throws QueryException {
        List<Client> clients = query.getClientList();

        clients.stream()
                .filter(client -> client.isOnlyInServergroup(config.getInt("groups.guest[@id]")))
                .filter(client -> client.getTimeConnected() > config.getLong("groups.new[@timeconnected]"))
                .forEach(client -> {
                    try {
                        query.servergroupAddClient(client.getDatabaseId(), config.getInt("groups.new[@id]"));
                    } catch (QueryException e) {
                        log.error(e.getMessage(), e);
                    }
                });

        clients.stream()
                .filter(client -> client.isInServergroup(config.getInt("groups.new[@id]")))
                .filter(client -> !client.isOnlyInServergroup(config.getInt("groups.new[@id]")))
                .forEach(client -> {
                    try {
                        query.servergroupDeleteClient(client.getDatabaseId(), config.getInt("groups.new[@id]"));
                    } catch (QueryException e) {
                        log.error(e.getMessage(), e);
                    }
                });

        List<Client> newClients = getNewClientList();
        if (newClients.size() == 0) return;

        List<String> messages = createMessagesForAdmins(newClients);

        List<Client> admins = clients.stream()
                .filter(client -> client.isInServergroup(config.getIntSet("groups.admins[@id]")))
                .filter(client -> !client.isInServergroup(config.getInt("groups.ignored[@id]")))
                .collect(Collectors.toList());

        for (Client client : admins) {
            try {
                query.sendTextMessageToClient(client.getId(), messages);
            } catch (QueryException e) {
                log.error(String.format("Failed to send message to client %s", client), e);
            }
        }
    }

    private List<Client> getNewClientList() throws QueryException {
        return query.getClientList().stream()
                .filter(client -> client.isInServergroup(config.getInt("groups.new[@id]")))
                .collect(Collectors.toList());
    }

    private List<String> createMessagesForAdmins(List<Client> newClients) {
        List<String> result = new LinkedList<>();

        String template = "[URL=client://%d/%s]%s[/URL]";
        StringBuilder builder = new StringBuilder(config.getString("messages.newclients[@start]").replace("$COUNT$", Integer.toString(newClients.size())));
        for (Client client : newClients) {
            String newClientInfo = String.format(template, client.getId(), client.getUniqueId(), client.getNickname());

            String built = builder.toString();
            if (built.getBytes().length + newClientInfo.getBytes().length > 1000) {
                result.add(built);
                builder = new StringBuilder("| ");
            }

            builder.append(newClientInfo).append(" | ");
        }

        result.add(builder.toString());

        return result;
    }

    @Teamspeak3Command("!reg")
    @ClientGroupAccess("servergroups.admins")
    public CommandResponse checkForNewClients(Client client, String params) throws QueryException {
        List<Client> newClients = getNewClientList();

        if (newClients.size() == 0)
            return new CommandResponse(config.getString("messages.newclients[@no]"));

        List<String> messages = createMessagesForAdmins(newClients);
        return new CommandResponse(messages.toArray(new String[]{}));
    }
}
