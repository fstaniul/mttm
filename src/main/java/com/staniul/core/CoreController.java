package com.staniul.core;

import com.staniul.configuration.ConfigurationLoader;
import com.staniul.configuration.annotations.ConfigFile;
import com.staniul.core.commands.Command;
import com.staniul.core.commands.CommandContainer;
import com.staniul.core.commands.CommandExecuteStatus;
import com.staniul.core.commands.CommandResponse;
import com.staniul.core.events.Event;
import com.staniul.core.events.EventContainer;
import com.staniul.core.security.AccessCheck;
import com.staniul.core.security.PermitAllAccessCheck;
import com.staniul.query.Client;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Controller of teamspeak 3 behaviour, that is events and commands. All commands called on teamspeak 3 server must be
 * registered within this controller to be invoked. All events that should be aware of clients joining and leaving
 * teamspeak 3 server should be registered within this controller.
 */
@Component
@ConfigFile("core.xml")
public class CoreController {
    private XMLConfiguration config;

    private HashMap<String, CommandContainer> commands;
    private List<EventContainer<Client>> enterEvents;
    private List<EventContainer<Integer>> leaveEvents;

    @Autowired
    public CoreController() throws ConfigurationException {
        config = ConfigurationLoader.load(CoreController.class);
        commands = new HashMap<>();
        enterEvents = new LinkedList<>();
        leaveEvents = new LinkedList<>();
    }

    /**
     * Register command within this controller without checking the access.
     *
     * @param commandName Name of command that clients can call.
     * @param command     Command to be executed when called.
     */
    public void registerCommand(String commandName, Command command) {
        registerCommand(commandName, command, new PermitAllAccessCheck<>());
    }

    /**
     * Registers command within this controller with access checker that checks if command can be called by this client
     * or not.
     *
     * @param commandName Name of command that client can call.
     * @param command     Command to be executed when called.
     * @param accessCheck Access checker to check if client has access to command or not.
     */
    public void registerCommand(String commandName, Command command, AccessCheck<Client> accessCheck) {
        CommandContainer cc = new CommandContainer(command, accessCheck);
        commands.putIfAbsent(commandName, cc);
    }

    /**
     * Registers event within this controller.
     *
     * @param event Event to be executed when teamspeak 3 event occurs.
     */
    public void registerEnterEvent(Event<Client> event) {
        registerEnterEvent(event, new PermitAllAccessCheck<>());
    }

    /**
     * Registers event within this controller with access check that filters event to be called only.
     *
     * @param event       Event to be executed when teamspeak 3 event occurs.
     * @param accessCheck Access check to check if event should be executed for this client or not.
     */
    public void registerEnterEvent(Event<Client> event, AccessCheck<Client> accessCheck) {
        EventContainer<Client> ec = new EventContainer<>(event, accessCheck);
        enterEvents.add(ec);
    }

    /**
     * Registers event within this controller.
     *
     * @param event Event to be executed when teamspeak 3 event occurs.
     */
    public void registerLeaveEvent(Event<Integer> event) {
        EventContainer<Integer> ec = new EventContainer<>(event, new PermitAllAccessCheck<>());
        leaveEvents.add(ec);
    }

    /**
     * Calls command with given name.
     *
     * @param command Name of command that should be called.
     * @param client  Client that is calling command.
     * @param params  Parameters that have been passed by client.
     *
     * @return Status of command execution.
     */
    public CommandResponse callCommand(String command, Client client, String params) {
        Configuration config = this.config.configurationAt("commands.messages");
        CommandContainer cc = commands.get(command);

        if (cc == null) {
            return new CommandResponse(CommandExecuteStatus.NOT_FOUND, config.getString(CommandExecuteStatus.NOT_FOUND.toString().toLowerCase()));
        }

        CommandResponse cr = cc.invoke(client, params);

        if (cr.getStatus() != CommandExecuteStatus.SUCCESSFUL)
            cr.setResponse(config.getString(cr.getStatus().toString().toLowerCase()));

        return cr;
    }
}
