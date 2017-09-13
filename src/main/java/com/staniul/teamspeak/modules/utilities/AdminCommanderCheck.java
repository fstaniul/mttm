package com.staniul.teamspeak.modules.utilities;

import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.teamspeak.query.client.Platform;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class AdminCommanderCheck {
    private static Logger log = LogManager.getLogger(AdminCommanderCheck.class);

    private CustomXMLConfiguration config;
    private final Query query;

    public AdminCommanderCheck(Query query) {
        this.query = query;
    }

    public void checkChannelCommander () throws QueryException {
        log.info("Poking admins about channel commander!");

        List<Client> admins = query.getClientList().stream()
                .filter(a -> a.isInServergroup(config.getIntSet("groups")))
                .filter(a -> a.getPlatform() == Platform.Windows)
                .filter(a -> !a.isCommander())
                .collect(Collectors.toList());

        for (Client admin : admins) {
            query.pokeClient(admin.getId(), config.getString("message"), false);
        }

        log.info("Finished poking admins about channel commander!");
    }
}
