package com.staniul.modules.clientscommands;

import com.staniul.security.clientaccesscheck.ClientChannelgroupAccessCheck;
import com.staniul.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@UseConfig("modules/jl.xml")
public class JoinLink {
    @WireConfig
    private CustomXMLConfiguration config;
    private Query query;

    @Autowired
    public JoinLink(Query query) {
        this.query = query;
    }

    @Teamspeak3Command("!jl")
    @ClientGroupAccess(value = "channelgroups.admins", check = ClientChannelgroupAccessCheck.class)
    public CommandResponse sendClientJoinLink (Client client, String params) {
        String message = "ts3server://" + config.getString("teamspeak[@addr]");
        message += "?cid=" + client.getCurrentChannelId();
        message += "".equals(params) ? "" : "&channel_password=" + params;

        return new CommandResponse(message);
    }
}
