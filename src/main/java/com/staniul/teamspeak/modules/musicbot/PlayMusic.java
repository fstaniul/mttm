package com.staniul.teamspeak.modules.musicbot;

import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.commands.validators.ValidateParams;
import com.staniul.teamspeak.modules.messengers.NotEmptyParamsValidator;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@UseConfig("modules/play-music.xml")
public class PlayMusic {

    @WireConfig
    private CustomXMLConfiguration config;
    private Query query;

    @Autowired
    public PlayMusic (Query query) {
        this.query = query;
    }

    @Teamspeak3Command("!play")
    @ClientGroupAccess("servergroups.admins")
    @ValidateParams(NotEmptyParamsValidator.class)
    public CommandResponse playCommand (Client client, String params) throws QueryException {
        List<Integer> botIds = config.getIntList("bot-ids");
        List<Client> bots = query.getClientList().stream().filter(c -> botIds.contains(c.getDatabaseId())).collect(Collectors.toList());
        Matcher matcher = Pattern.compile("^(?:(\\d+) )?(.*)$").matcher(params);

        if (matcher.find()) {
            int delay = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : 100;
            String title = matcher.group(2);

            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(delay);
                    query.sendTextMessageToClient(bots.get(1).getId(), "!play " + title);
                } catch (Exception e) {}
            });

            query.sendTextMessageToClient(bots.get(0).getId(), "!play " + title);
            thread.start();

            return new CommandResponse(config.getString("messages.success")
                    .replace("%TITLE%", title)
                    .replace("%DELAY%", Integer.toString(delay))
            );
        }
        else {
            return new CommandResponse(config.getString("messages.invalid-params"));
        }
    }
}
