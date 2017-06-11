package com.staniul.teamspeak.commands;

import com.staniul.configuration.ConfigurationLoader;
import com.staniul.configuration.annotations.ConfigFile;
import com.staniul.query.Client;
import com.staniul.query.Query;
import com.staniul.query.QueryException;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.log4j.Logger;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Command messages send command responses to clients after commands were executed, whether it was successful or not.
 */
@Component
@Aspect
@ConfigFile("commandmessages.xml")
public class CommandMessenger {
    private static Logger log = Logger.getLogger(CommandMessenger.class);

    private XMLConfiguration config;
    private Query query;

    @Autowired
    public CommandMessenger (Query query) throws ConfigurationException {
        config = ConfigurationLoader.load(CommandMessenger.class);
        this.query = query;
    }

    @Pointcut(value = "execution(com.staniul.teamspeak.commands.CommandResponse * (com.staniul.query.Client, java.lang.String)) && " +
            "args(client,params)", argNames = "client, params")
    public void commandExecution (Client client, String params) {

    }

    @AfterReturning(value = "commandExecution(client,params)", returning = "response", argNames = "client,params,response")
    public void sendMessageAfterCommandReturn (Client client, String params, CommandResponse response) {
        if (response.getStatus() != CommandExecutionStatus.EXECUTED_SUCCESSFULLY) {
            response.setMessage(new String[]{ config.getString(response.getStatus().toString().toLowerCase()) });
        }

//        try {
//            query.sendTextMessageToClient(client.getId(), response.getMessage());
//        } catch (QueryException e) {
//            log.error("Failed to send message to client!", e);
//        }
        log.info(Arrays.toString(response.getMessage()));
    }

    @AfterThrowing(value = "commandExecution(client,params)", argNames = "client,params")
    public void sendMessageAfterCommandThrow (Client client, String params) {
        String message = config.getString(CommandExecutionStatus.EXECUTION_TERMINATED.toString().toLowerCase());
//        try {
//            query.sendTextMessageToClient(client.getId(), message);
//        } catch (QueryException e) {
//            log.error("Failed to send message to client!", e);
//        }
        log.info(message);
    }
}