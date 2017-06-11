package com.staniul.teamspeak.modules;

import com.staniul.query.Client;
import com.staniul.teamspeak.Teamspeak3Module;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import org.springframework.stereotype.Component;

@Component
@Teamspeak3Module
public class DummyTestModule {
    @Teamspeak3Command("nana")
    public CommandResponse everyOneCanCallMe (Client client, String params) {
        System.out.println("I AM BEING CALLED!");
        return new CommandResponse("I've been called!");
    }
}
