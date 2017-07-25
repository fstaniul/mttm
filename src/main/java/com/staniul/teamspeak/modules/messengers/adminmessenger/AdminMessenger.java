package com.staniul.teamspeak.modules.messengers.adminmessenger;

import com.staniul.teamspeak.query.Client;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

public interface AdminMessenger {
    List<Message> listMessages ();

    String[] addMessage (int clientDatabaseId, String message);

    String[] deleteMessage(int messageId, Client client);

    String[] deleteMessage(String messagePrefix, Client client);

    class Message {
        static RowMapper<Message> rowMapper() {
            return (resultSet, i) -> new Message(
                    resultSet.getInt("id"),
                    resultSet.getInt("client_id"),
                    resultSet.getString("message")
            );
        }

        int id;
        int clientId;
        String message;

        Message(int id, int clientId, String message) {
            this.id = id;
            this.clientId = clientId;
            this.message = message;
        }

        public int getId() {
            return id;
        }

        public int getClientId() {
            return clientId;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return message + " @" + clientId;
        }
    }
}
