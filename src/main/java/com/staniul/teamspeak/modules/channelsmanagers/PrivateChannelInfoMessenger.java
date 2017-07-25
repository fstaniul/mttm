package com.staniul.teamspeak.modules.channelsmanagers;

import com.staniul.teamspeak.events.EventType;
import com.staniul.teamspeak.events.Teamspeak3Event;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@UseConfig("modules/pcim.xml")
public class PrivateChannelInfoMessenger {
    private static Logger log = LogManager.getLogger(PrivateChannelInfoMessenger.class);

    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;
    private final JdbcTemplate database;

    @Autowired
    public PrivateChannelInfoMessenger(Query query, JdbcTemplate database) {
        this.query = query;
        this.database = database;
    }

    @Teamspeak3Event(EventType.JOIN)
    public synchronized void sendMessagesToClientOnJoin(Client client) {
        List<Message> messages = database.query(
                "SELECT * FROM private_channels_messages WHERE client_id = ?",
                Message.rowMapper(),
                client.getDatabaseId()
        );

        if (messages.size() > 0) {
            StringBuilder messageToSend = new StringBuilder(
                    config.getString("messages.info-messages[@header]")
                            .replace("$NUMBER$", Integer.toString(messages.size()))
            );

            for (Message message : messages) {
                messageToSend.append("\n")
                        .append(" - ")
                        .append(message.getMessage());
            }

            try {
                query.sendTextMessageToClient(client.getId(), messageToSend.toString(), "\n");
            } catch (QueryException e) {
                log.error("Failed to send message to client about his private channel.", e);
            }

            database.update("DELETE FROM private_channels_messages WHERE client_id = ?", client.getDatabaseId());
        }
    }

    public synchronized void addMessageToClient(int clientDatabaseId, String message) {
        try {
            Client clientOnline = query.getClientList().stream()
                    .filter(client -> client.getDatabaseId() == clientDatabaseId)
                    .findAny()
                    .orElse(null);

            if (clientOnline != null) {
                query.sendTextMessageToClient(clientOnline.getId(), message);
                return;
            }
        } catch (QueryException e) {
            log.error("Failed to send message about channel to client!", e);
        }

        database.update("INSERT INTO private_channels_messages (client_id, message) VALUES (?, ?)",
                clientDatabaseId, message);
    }

    private static class Message {
        static RowMapper<Message> rowMapper() {
            return ((resultSet, i) -> new Message(
                    resultSet.getInt("id"),
                    resultSet.getInt("client_id"),
                    resultSet.getString("message")
            ));
        }

        private int id;
        private int clientDatabaseId;
        private String message;

        Message(int id, int clientDatabaseId, String message) {
            this.id = id;
            this.clientDatabaseId = clientDatabaseId;
            this.message = message;
        }

        public int getId() {
            return id;
        }

        public int getClientDatabaseId() {
            return clientDatabaseId;
        }

        public String getMessage() {
            return message;
        }
    }
}
