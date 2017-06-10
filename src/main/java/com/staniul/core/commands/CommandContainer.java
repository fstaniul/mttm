package com.staniul.core.commands;

import com.staniul.core.security.AccessCheck;
import com.staniul.query.Client;
import org.apache.log4j.Logger;

/**
 * Container for command that before each invocation of command checks if client has access to this command. Used by
 * {@link com.staniul.core.CoreController CoreController} to invoke commands that are being called by teamspeak 3
 * users.
 */
public class CommandContainer {
    private static Logger log = Logger.getLogger(CommandContainer.class);

    private Command command;
    private AccessCheck<Client> clientAccessCheck;

    public CommandContainer(Command command, AccessCheck<Client> clientAccessCheck) {
        this.command = command;
        this.clientAccessCheck = clientAccessCheck;
    }

    /**
     * Checks if client has access to command and then invokes it.
     *
     * @param client     Client that is invoking command.
     * @param parameters Parameters passed along with command
     *
     * @return {@code true} if client has access to command, {@code false} otherwise.
     */
    public CommandResponse invoke(Client client, String parameters) {
        if (clientAccessCheck.apply(client)) {
            try {
                return command.invoke(client, parameters);
            } catch (Exception e) {
                return new CommandResponse(CommandExecuteStatus.EXECUTION_ERROR, "");
            }
        }
        return new CommandResponse(CommandExecuteStatus.ACCESS_DENIED, "");
    }
}
