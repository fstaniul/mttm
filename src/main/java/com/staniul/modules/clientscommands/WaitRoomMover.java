package com.staniul.modules.clientscommands;

import com.staniul.modules.messengers.NotEmptyParamsValidator;
import com.staniul.security.clientaccesscheck.ClientChannelgroupAccessCheck;
import com.staniul.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.commands.validators.ValidateParams;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@UseConfig("modules/wrm.xml")
public class WaitRoomMover {
    private static Logger log = LogManager.getLogger(WaitRoomMover.class);

    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;

    @Autowired
    public WaitRoomMover (Query query) {
        this.query = query;
    }

    @Teamspeak3Command("!m")
    @ClientGroupAccess(value = "channelgroups.admins", check = ClientChannelgroupAccessCheck.class)
    @ValidateParams(NotEmptyParamsValidator.class)
    public CommandResponse moveClients (Client client, String params) throws QueryException {
        String lcp = params.toLowerCase();

        List<Client> clients = query.getClientList().stream()
                .filter(c -> c.getCurrentChannelId() == config.getInt("channel[@id]"))
                .collect(Collectors.toList());

        List<Client> matchedClients = clients.stream()
                .filter(c -> c.getNickname().toLowerCase().startsWith(lcp))
                .collect(Collectors.toList());

        Client exactMatch = matchedClients.stream()
                .filter(c -> c.getNickname().toLowerCase().equals(lcp))
                .findFirst()
                .orElse(null);

        if (matchedClients.size() > 1 && exactMatch == null) {
            int paramsSize = lcp.length();
            List<String> highlightedNicknames = matchedClients.stream()
                    .map(c -> "[b]" + c.getNickname().substring(0, paramsSize) + "[/b]" +
                                    (c.getNickname().length() > paramsSize + 1 ? c.getNickname().substring(paramsSize + 1) : ""))
                    .collect(Collectors.toList());

            String msg = config.getString("messages.more[@msg]")
                    .replace("$PARAMS$", params)
                    .replace("$OPTIONS$", String.join(", ", highlightedNicknames));
            return new CommandResponse(msg);
        }

        else if (matchedClients.size() == 1 || exactMatch != null) {
            if (exactMatch == null) exactMatch = matchedClients.get(0);
            query.moveClient(exactMatch.getId(), client.getCurrentChannelId());
            query.sendTextMessageToClient(exactMatch.getId(), config.getString("messages.moved[@target]").replace("$NICKNAME$", client.getNickname()));
            return new CommandResponse(config.getString("messages.moved[@msg]").replace("$NICKNAME$", exactMatch.getNickname()));
        }

        else {
            return new CommandResponse(config.getString("messages.not-found[@msg]").replace("$PARAMS$", params));
        }
    }
}
