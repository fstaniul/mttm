package com.staniul.teamspeak.modules.messengers.adminmessenger;

import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.commands.validators.IntegerParamsValidator;
import com.staniul.teamspeak.commands.validators.ValidateParams;
import com.staniul.teamspeak.events.EventType;
import com.staniul.teamspeak.events.Teamspeak3Event;
import com.staniul.teamspeak.modules.messengers.NotEmptyParamsValidator;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.ClientDatabase;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@UseConfig("modules/am.xml")
public class AdminMessengerImpl implements AdminMessenger {
    private static Logger log = LogManager.getLogger(AdminMessengerImpl.class);

    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;
    private final JdbcTemplate database;

    @Autowired
    public AdminMessengerImpl(Query query, JdbcTemplate database) {
        this.query = query;
        this.database = database;
    }

    @Override
    public List<Message> listMessages() {
        return database.query(
                "SELECT * FROM admin_messages",
                Message.rowMapper()
        );
    }

    @Override
    public String[] addMessage(int clientDatabaseId, String message) {
        if (message.length() > 900)
            return new String[]{config.getString("messages.add.too-long")};

        database.update("INSERT INTO admin_messages (client_id, message) VALUES (?, ?)",
                clientDatabaseId, message);

        sendMessageToAllAdmins(clientDatabaseId, message);

        return new String[]{config.getString("messages.add.success")};
    }

    @Override
    public String[] deleteMessage(int messageId, Client client) {
        int affectedRows = database.update(
                "DELETE FROM admin_messages WHERE id = ? AND client_id = ?",
                messageId, client.getDatabaseId()
        );

        if (affectedRows > 0) {
            return new String[]{config.getString("messages.delete.success-nomsg")};
        }
        else {
            return new String[]{config.getString("messages.delete.not-found")};
        }
    }

    @Override
    public String[] deleteMessage(String messagePrefix, Client client) {
        String queryParameter = messagePrefix + "%";

        List<Message> messages = database.query(
                "SELECT * FROM admin_messages WHERE message LIKE ? AND client_id = ?",
                Message.rowMapper(),
                queryParameter, client.getDatabaseId()
        );

        if (messages.size() == 1) {
            Message message = messages.get(0);
            database.update("DELETE FROM admin_messages WHERE id = ?", message.getId());

            return new String[]{
                    config.getString("messages.delete.success")
                            .replace("$MSG$", message.getMessage())
            };
        }

        else if (messages.size() == 0) {
            return new String[]{config.getString("messages.delete.not-found")};
        }

        else {
            List<String> messagesToSend = new ArrayList<>();
            messagesToSend.add(
                    config.getString("messages.delete.found-more")
                            .replace("$NUMBER$", Integer.toString(messages.size()))
            );

            for (Message message : messages) {
                messagesToSend.add(
                        message.getId() + ". " + message.getMessage()
                );
            }

            return messagesToSend.toArray(new String[messages.size()]);
        }
    }

    private void sendMessageToAllAdmins(int clientDatabaseId, String message) {
        try {
            ClientDatabase clientDatabaseInfo = query.getClientDatabaseInfo(clientDatabaseId);
            Set<Integer> adminGroups = config.getIntSet("groups.admins");
            List<Client> onlineAdmins = query.getClientList().stream()
                    .filter(client -> client.isInServergroup(adminGroups))
                    .filter(client -> client.getDatabaseId() != clientDatabaseId)
                    .collect(Collectors.toList());

            String[] messageToSend = new String[]{
                    config.getString("messages.templates.message")
                            .replace("$MSG$", message)
                            .replace("$AUTHOR$", clientDatabaseInfo.getNickname())
            };

            for (Client admin : onlineAdmins)
                query.sendTextMessageToClient(admin.getId(), messageToSend);
        } catch (QueryException e) {
            log.error("Failed to retrieve information from teamspeak 3 server and send message to administrators.", e);
        }
    }

    @Teamspeak3Command("!msgadd")
    @ClientGroupAccess("servergroups.admins")
    @ValidateParams(NotEmptyParamsValidator.class)
    public CommandResponse addMessageCommand(Client client, String params) {
        return new CommandResponse(
                addMessage(client.getDatabaseId(), params)
        );
    }

    @Teamspeak3Command("!msgdel")
    @ClientGroupAccess("servergroups.admins")
    @ValidateParams(NotEmptyParamsValidator.class)
    public CommandResponse delMessageCommand(Client client, String params) {
        //Client gave a digit
        if (params.matches("\\d+")) {
            return new CommandResponse(
                    deleteMessage(Integer.parseInt(params), client)
            );
        }

        else {
            return new CommandResponse(
                    deleteMessage(params, client)
            );
        }
    }

    @Teamspeak3Command("!msgshow")
    @ClientGroupAccess("servergroups.admins")
    public CommandResponse showMessagesCommand(Client client, String params) {
        List<Message> messages = listMessages();
        if (messages.size() == 0) {
            return new CommandResponse(
                    config.getString("messages.show.nothing")
            );
        }

        else {
            String messageTemplate;
            if (params.matches("-v")) messageTemplate = config.getString("messages.templates.show.verbose");
            else messageTemplate = config.getString("messages.templates.show.standard");

            List<String> messageToSend = new ArrayList<>();
            messageToSend.add(
                    config.getString("messages.show.header")
                            .replace("$NUMBER$", Integer.toString(messages.size()))
            );

            for (Message message : messages) {
                messageToSend.add(
                        messageTemplate.replace("$MSG$", message.getMessage())
                                .replace("$ID$", Integer.toString(message.getId()))
                );
            }

            return new CommandResponse(messageToSend.toArray(new String[messageToSend.size()]));
        }
    }

    @Teamspeak3Command("!msgdela")
    @ClientGroupAccess("servergroups.headadmins")
    @ValidateParams(IntegerParamsValidator.class)
    public CommandResponse delMessageCommandAdmin(Client client, String params) {
        int affectedRows = database.update(
                "DELETE FROM admin_messages WHERE id = ?",
                Integer.parseInt(params)
        );

        if (affectedRows > 0) {
            return new CommandResponse(
                    config.getString("messages.delete-admin.success")
            );
        }

        else {
            return new CommandResponse(
                    config.getString("messages.delete-admin.not-found")
            );
        }
    }

    @Teamspeak3Event(EventType.JOIN)
    @ClientGroupAccess("servergroup.admins")
    public void sendMessagesToAdminOnJoin(Client client) throws QueryException {
        List<Message> messages = listMessages();
        if (messages.size() > 0) {
            Map<Integer, ClientDatabase> authors = getMessageAuthors(messages);
            List<String> messagesToSend = prepareMessagesToSend(messages, authors);
            query.sendTextMessageToClient(client.getId(),
                    config.getString("messages.event.header")
                            .replace("$NUMBER$", Integer.toString(messages.size()))
            );
            query.sendTextMessageToClient(client.getId(), messagesToSend);
        }
    }

    private List<String> prepareMessagesToSend(List<Message> messages, Map<Integer, ClientDatabase> authors) {
        List<String> messagesToSend = new ArrayList<>();
        String template = config.getString("messages.templates.message");
        for (Message message : messages) {
            messagesToSend.add(
                    template.replace("$MSG$", message.getMessage())
                            .replace("$AUTHOR$", authors.get(message.getClientId()).getNickname())
            );
        }
        return messagesToSend;
    }

    private Map<Integer, ClientDatabase> getMessageAuthors(List<Message> messages) throws QueryException {
        Map<Integer, ClientDatabase> authors = new HashMap<>();
        for (Message message : messages) {
            if (!authors.containsKey(message.getClientId())) {
                ClientDatabase author = query.getClientDatabaseInfo(message.getClientId());
                authors.put(message.getClientId(), author);
            }
        }
        return authors;
    }
}
