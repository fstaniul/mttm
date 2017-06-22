package com.staniul.teamspeak.commands;

import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@UseConfig("cmdmsg.xml")
public class CommandMessenger {
    private static Logger log = LogManager.getLogger(CommandMessenger.class);

    @WireConfig
    private XMLConfiguration config;
    private final Query query;

    @Autowired
    public CommandMessenger(Query query) {
        this.query = query;
    }

    public void sendResponseToClient (Client client, CommandResponse response) {
        if (response.getStatus() != CommandExecutionStatus.EXECUTED_SUCCESSFULLY)
            response.setMessage(new String[]{ config.getString(response.getStatus().toString().toLowerCase()) });

        if (response.getMessage() != null) {
            try {
                query.sendTextMessageToClient(client.getId(), response.getMessage());
            } catch (QueryException e) {
                log.error(String.format("Failed to send message to client (%d), messages %s",  client.getDatabaseId(), Arrays.toString(response.getMessage())));
            }
        }
    }
}
