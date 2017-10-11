package com.staniul.teamspeak.modules.adminlists;

import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.ClientDatabase;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@UseConfig("modules/afl.xml")
public class AdminOfflineList {
    private static Logger log = LogManager.getLogger(AdminOfflineList.class);

    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;

    private List<Servergroup2> servergroup2s;

    @Autowired
    public AdminOfflineList(Query query) {
        this.query = query;
    }

    @PostConstruct
    private void init() {
        servergroup2s = config.getClasses(Servergroup2.class, "groups.servergroup2");
    }

    @Teamspeak3Command("!refafl")
    @ClientGroupAccess("servergroups.headadmins")
    public CommandResponse refreshAdminOfflineList(Client client, String params) throws QueryException {
        refreshAdminList();
        return new CommandResponse(config.getString("commands.refafl[@response]"));
    }

    public void refreshAdminList() throws QueryException {
        Map<Servergroup2, List<ClientDatabase>> data = new HashMap<>();

        for (Servergroup2 servergroup2 : servergroup2s) {
            data.put(servergroup2, !servergroup2.isSolo() ?
                            query.getClientDatabaseListInServergroup(servergroup2.getId()) :
                            query.getClientDatabaseListInServergroup(servergroup2.getId()).subList(0, 1)
            );
        }

        refreshDisplay(data);
    }

    private void refreshDisplay(Map<Servergroup2, List<ClientDatabase>> data) throws QueryException {
        int count = 0;
        for (Map.Entry<Servergroup2, List<ClientDatabase>> entry : data.entrySet()) {
            count += entry.getValue().size();
        }
        StringBuilder description = new StringBuilder();
        for (Servergroup2 servergroup2 : servergroup2s) {
            description.append("[IMG]").append(servergroup2.getIcon()).append("[/IMG]")
                    .append(config.getString("display[@header]").replace("$HEADER$", servergroup2.getName()))
                    .append("\n");
            data.get(servergroup2).sort(Comparator.comparing(ClientDatabase::getNickname, Comparator.comparing(String::toLowerCase)));
            for (ClientDatabase admin : data.get(servergroup2)) {
                description.append(config.getString("display[@listsign]"))
                        .append(config.getString("display[@entry]").replace("$ENTRY$", admin.getNickname()))
                        .append("\n");
            }
            description.append("\n");
        }

        description.append(config.getString("display[@count]").replace("%NUMBER%", Integer.toString(count))).append("\n");
        query.channelChangeDescription(description.toString(), config.getInt("display[@id]"));
    }
}
