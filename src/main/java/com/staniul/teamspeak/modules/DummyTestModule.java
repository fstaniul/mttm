package com.staniul.teamspeak.modules;

import com.staniul.query.Client;
import com.staniul.teamspeak.Teamspeak3Module;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.events.EventType;
import com.staniul.teamspeak.events.Teamspeak3Event;
import com.staniul.teamspeak.security.clientaccesscheck.GroupAccess;
import org.springframework.stereotype.Component;

@Component
@Teamspeak3Module
public class DummyTestModule {
    @Teamspeak3Command(value = "nana")
    @GroupAccess({12})
    public CommandResponse everyOneCanCallMe (Client client, String params) {
        System.out.println("I AM BEING CALLED!");
        return new CommandResponse("I've been called!");
    }

    @Teamspeak3Event(EventType.JOIN)
    public void joinEvent (Client client) {
        System.out.println("JOIN EVENT");
    }
}
