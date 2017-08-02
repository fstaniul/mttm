package com.staniul.teamspeak.modules.registerers;

import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.teamspeak.taskcontroller.Task;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

    @Task(delay = 3 * 60 * 1000)
    public void checkForNewClients() throws QueryException {
        log.info("Looking for new clients on teamspeak 3 server!");

        List<Client> clients = query.getClientList();

        int guestGroupId = config.getInt("groups.guest[@id]");
        int newGroupId = config.getInt("groups.new[@id]");
        long tc = config.getLong("groups.new[@timeconnected]");

        //Add clients to a new group that are currently guests and are connected for enuff time on teamspeak 3 server:
        clients.stream().filter(c -> c.isInServergroup(guestGroupId) && c.getTimeConnected() > tc).forEach(c -> {
            try {
                query.servergroupAddClient(c.getDatabaseId(), newGroupId);
            } catch (QueryException ignore) {
            }
        });

        //Remove clients from new group that has been already registered:
        clients.stream().filter(c -> !c.isOnlyInServergroup(newGroupId) && c.isInServergroup(newGroupId)).forEach(c -> {
            try {
                query.servergroupDeleteClient(c.getDatabaseId(), newGroupId);
            } catch (QueryException ignore) {
            }
        });
    }

    @Task(delay = 30 * 60 * 1000)
    public void messageAdminsWithNewClientsList() throws QueryException {
        List<Client> admins = query.getClientList().stream()
                .filter(client -> client.isInServergroup(config.getIntSet("groups.admins[@id]")))
                .filter(client -> !client.isInServergroup(config.getIntSet("groups.admins[@ignored]")))
                .collect(Collectors.toList());

        List<Client> newClients = getNewClientList();
        if (newClients.size() > 0) {
            List<String> messages = createMessagesForAdminsAboutNewClients(newClients);

            for (Client client : admins) {
                try {
                    query.sendTextMessageToClient(client.getId(), messages);
                } catch (QueryException e) {
                    log.error(String.format("Failed to send message to client %s", client), e);
                }
            }
        }

        List<Client> errorRegs = getErrorClients();
        if (errorRegs.size() > 0) {
            List<String> errors = errorRegInfo(errorRegs);

            for (Client admin : admins) {
                try {
                    query.sendTextMessageToClient(admin.getId(), errors);
                } catch (QueryException e) {
                    log.error("Failed to send text message to admin " + admin.getDatabaseId(), e);
                }
            }
        }
    }

    private List<Client> getNewClientList() throws QueryException {
        return query.getClientList().stream().filter(client -> client.isInServergroup(config.getInt("groups.new[@id]")))
                .collect(Collectors.toList());
    }

    private List<Client> getErrorClients() throws QueryException {
        Set<Integer> registered = config.getIntSet("groups.register[@id]");
        Set<Integer> age = config.getIntSet("groups.age[@id]");
        return query.getClientList().stream().filter(c -> (c.isInServergroup(registered) && !c.isInServergroup(age))
                || (c.isInServergroup(age) && !c.isInServergroup(registered))).collect(Collectors.toList());
    }

    private List<String> createMessagesForAdminsAboutNewClients(List<Client> newClients) {
        String message = config.getString("messages.newclients[@start]").replace("$COUNT$",
                Integer.toString(newClients.size()));

        return concatenateClients(newClients, message);
    }

    private List<String> errorRegInfo(List<Client> clients) {
        String message = config.getString("messages.errorclients[@start]").replace("$NUMBER$",
                Integer.toString(clients.size()));

        return concatenateClients(clients, message);
    }

    private List<String> concatenateClients(List<Client> clients, String startingMessage) {
        List<String> result = new ArrayList<>();

        String template = "[URL=client://%d/%s]%s[/URL]";
        StringBuilder sb = new StringBuilder(startingMessage);

        for (Client client : clients) {
            String newClientInfo = String.format(template, client.getId(), client.getUniqueId(), client.getNickname());

            String built = sb.toString();
            if (built.getBytes().length + newClientInfo.getBytes().length > 1000) {
                result.add(built);
                sb = new StringBuilder("| ");
            }

            sb.append(newClientInfo).append(" | ");
        }

        result.add(sb.toString());

        return result;
    }

    @Teamspeak3Command("!reg")
    @ClientGroupAccess("servergroups.admins")
    public CommandResponse checkForNewClients(Client client, String params) throws QueryException {
        List<Client> newClients = getNewClientList();
        List<Client> errorClients = getErrorClients();

        if (newClients.size() == 0 && errorClients.size() == 0)
            return new CommandResponse(config.getString("messages.newclients[@no]"));

        List<String> messages = new ArrayList<>();

        if (newClients.size() > 0)
            messages.addAll(createMessagesForAdminsAboutNewClients(newClients));

        if (errorClients.size() > 0)
            messages.addAll(errorRegInfo(errorClients));

        return new CommandResponse(messages.toArray(new String[] {}));
    }
}
