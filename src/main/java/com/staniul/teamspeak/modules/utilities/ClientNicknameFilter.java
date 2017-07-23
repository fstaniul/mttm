package com.staniul.teamspeak.modules.utilities;

import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.commands.validators.ValidateParams;
import com.staniul.teamspeak.modules.messengers.NotEmptyParamsValidator;
import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.teamspeak.taskcontroller.Task;
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
import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    private final String regexFile = "./data/nickname-filters.txt";

    @Autowired
    public ClientNicknameFilter(Query query) {
        this.query = query;
    }

    @PostConstruct
    private void init() {
        patterns = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(regexFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null)
                patterns.add(Pattern.compile(".*" + line.toLowerCase() + ".*"));
        } catch (IOException e) {
            log.error("Failed to load file with nickname filters", e);
        }
    }

    @PreDestroy
    private void save() {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(regexFile, false), StandardCharsets.UTF_8)), true)) {
            for (Pattern pattern : patterns) {
                writer.println(patternToString(pattern));
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Failed to save nickname filters to file.", e);
        }
    }

    private String patternToString(Pattern pattern) {
        return pattern.pattern().substring(2, pattern.pattern().length() - 2);
    }

    public boolean filterClientNicknameOnJoin(Client client) {
        String lowerCaseNickname = client.getNickname().toLowerCase();
        boolean accept = true;
        for (Pattern pattern : patterns) {
            if (pattern.matcher(lowerCaseNickname).matches()) {
                accept = false;

                try {
                    query.kickClient(client.getId(), config.getString("messages.kick"));
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
                        query.kickClient(client.getId(), config.getString("messages.kick"));
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

    @Teamspeak3Command("!cnfadd")
    @ClientGroupAccess("servergroups.headadmins")
    @ValidateParams(NotEmptyParamsValidator.class)
    public CommandResponse addNewNicknameFilter(Client client, String params) {
        patterns.add(Pattern.compile(".*" + params + ".*"));
        return new CommandResponse(config.getString("commands.add").replace("$FILTER$", params));
    }

    @Teamspeak3Command("!cnfshow")
    @ClientGroupAccess("servergroups.headadmins")
    public CommandResponse showNicknameFilters(Client client, String params) {
        StringBuilder sb = new StringBuilder(config.getString("commands.show"));
        for (Pattern pattern : patterns) sb.append(" ").append(patternToString(pattern)).append(",");
        if (patterns.size() > 0) sb.delete(sb.length() - 1, sb.length());
        return new CommandResponse(sb.toString());
    }

    @Teamspeak3Command("!cnfdel")
    @ClientGroupAccess("servergroups.headadmins")
    @ValidateParams(NotEmptyParamsValidator.class)
    public CommandResponse deleteNicknameFilter(Client client, String params) {
        List<Pattern> pps = patterns.stream()
                .filter(p -> patternToString(p).startsWith(params))
                .collect(Collectors.toList());

        Pattern exact = pps.stream().filter(p -> patternToString(p).equals(params)).findAny().orElse(null);


        if (pps.size() == 1 || exact != null) {
            if (exact == null) exact = pps.get(0);

            patterns.remove(exact);
            return new CommandResponse(config.getString("commands.delete")
                    .replace("$FILTER$", patternToString(exact)));
        }

        if (pps.size() > 1) {
            StringBuilder response = new StringBuilder(config.getString("commands.delete[@more]"));
            for (Pattern pattern : pps)
                response.append(patternToString(pattern)).append(", ");
            response.delete(response.length() - 2, response.length());
            return new CommandResponse(response.toString());
        }

        return new CommandResponse(config.getString("commands.delete[@none]"));
    }
}
