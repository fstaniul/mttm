package com.staniul.teamspeak;

import com.staniul.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.query.Client;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.springframework.stereotype.Component;
import sun.misc.Cleaner;

import java.util.List;
import java.util.stream.Collectors;

@Component
@UseConfig("corecmd.xml")
public class CoreCommands {
    @WireConfig
    private CustomXMLConfiguration config;

    @Teamspeak3Command("!stop")
    @ClientGroupAccess("servergroups.headadmins")
    public CommandResponse stopCommand(Client client, String params) {
        System.exit(0);
        return new CommandResponse("Stopping!");
    }

    @Teamspeak3Command("!help")
    @ClientGroupAccess("servergroups.admins")
    public CommandResponse listCommands (Client client, String params) {
        List<Command> commands = config.getClasses(Command.class, "command-list.cmd");
        List<String> messages = commands.stream().map(c -> "[b]" + c.command + "[/b] - " + c.description).collect(Collectors.toList());
        return new CommandResponse(messages.toArray(new String[]{}));
    }

    public static class Command {
        private String command;
        private String description;

        public Command() {
        }

        public Command(String command, String description) {
            this.command = command;
            this.description = description;
        }

        public String getCommand() {
            return command;
        }

        public String getDescription() {
            return description;
        }
    }
}
