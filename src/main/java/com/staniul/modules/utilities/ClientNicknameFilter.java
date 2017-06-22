package com.staniul.modules.utilities;

import com.staniul.taskcontroller.Task;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@UseConfig("modules/cnf.xml")
public class ClientNicknameFilter {
    private static Logger log = LogManager.getLogger(ClientNicknameFilter.class);

    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;
    private List<Pattern> patterns;
    private Set<Client> warnedClients = new HashSet<>();

    @Autowired
    public ClientNicknameFilter(Query query) {
        this.query = query;
    }

    @PostConstruct
    private void init() {
        patterns = config.getList(String.class, "rules.rule[@regex]").stream()
                .map(Pattern::compile)
                .collect(Collectors.toList());
    }

    public boolean filterClientNicknameOnJoin(Client client) {
        String lowerCaseNickname = client.getNickname().toLowerCase();
        boolean accept = true;
        for (Pattern pattern : patterns) {
            if (pattern.matcher(lowerCaseNickname).matches()) {
                accept = false;

                try {
                    query.kickClient(client.getId(), config.getString("messages.kick[@reason]"));
                } catch (QueryException e) {
                    log.error(String.format("Failed to kick client (%d %s) from teamspeak 3 server, client nickname is prohibited!", client.getDatabaseId(), client.getNickname()), e);
                }

                break;
            }
        }

        return accept;
    }

    @Task(delay = 5 * 60 * 1000)
    public void checkClientNicknameWarnAndKick() throws QueryException {
        List<Client> clients = query.getClientList();
        for (Client client : clients) {
            String lowerCaseNickname = client.getNickname().toLowerCase();
            for (Pattern pattern : patterns) {
                if (pattern.matcher(lowerCaseNickname).matches()) {
                    if (warnedClients.contains(client)) {
                        warnedClients.remove(client);
                        query.kickClient(client.getId(), config.getString("messages.kick[@reason]"));
                    }
                    else {
                        warnedClients.add(client);
                        List<String> messages = config.getList(String.class, "messages.poke").stream()
                                .map(s -> s.replace("$NICKNAME$", client.getNickname()))
                                .collect(Collectors.toList());
                        query.pokeClient(client.getId(), messages);
                    }
                }
            }
        }
    }
}
