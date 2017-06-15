package com.staniul.teamspeak;

import com.staniul.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.query.Client;
import org.springframework.stereotype.Component;

@Component
public class CoreCommands {
    @Teamspeak3Command("!stop")
    @ClientGroupAccess("headAdministrators")
    public CommandResponse stopCommand(Client client, String params) {
        System.exit(0);
        return new CommandResponse("Stopping!");
    }
}
