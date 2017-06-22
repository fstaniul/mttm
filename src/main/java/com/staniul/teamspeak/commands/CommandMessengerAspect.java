package com.staniul.teamspeak.commands;

import com.staniul.teamspeak.query.Client;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Command messages send command responses to clients after command were executed, whether it was successful or not.
 */
@Component
@Aspect
public class CommandMessengerAspect {
    private static Logger log = LogManager.getLogger(CommandMessengerAspect.class);

    private final CommandMessenger messenger;

    @Autowired
    public CommandMessengerAspect(CommandMessenger messenger) {
        this.messenger = messenger;
    }

    @Pointcut(value = "execution(com.staniul.teamspeak.commands.CommandResponse * (com.staniul.teamspeak.query.Client,..)) && " +
            "@annotation(com.staniul.teamspeak.commands.Teamspeak3Command) && " +
            "args(client,..)", argNames = "client")
    public void commandExecution (Client client) {}

    @AfterReturning(value = "commandExecution(client)", returning = "response", argNames = "client,response")
    public void sendMessageAfterCommandReturn (Client client, CommandResponse response) {
        messenger.sendResponseToClient(client, response);
    }

    @AfterThrowing(value = "commandExecution(client)", argNames = "client")
    public void sendMessageAfterCommandThrow (Client client) {
        CommandResponse response = new CommandResponse(CommandExecutionStatus.EXECUTION_TERMINATED, null);
        messenger.sendResponseToClient(client, response);
    }
}
