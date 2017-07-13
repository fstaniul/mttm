package com.staniul.modules.utilities;

import com.staniul.taskcontroller.Task;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@UseConfig("modules/cc.xml")
public class AdminCommanderCheck {
    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;

    @Autowired
    public AdminCommanderCheck(Query query) {
        this.query = query;
    }

    @Task(delay = 5 * 60 * 1000)
    public void checkChannelCommander () throws QueryException {
        List<Client> admins = query.getClientList().stream()
                .filter(a -> a.isInServergroup(config.getIntSet("groups")))
                .filter(a -> !a.isCommander())
                .collect(Collectors.toList());

        for (Client admin : admins) {
            query.pokeClient(admin.getId(), config.getString("message"), false);
        }
    }
}
