package com.staniul.modules.messengers;

import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.util.*;

@Component
@UseConfig("modules/wm.xml")
public class WelcomeMessenger {
    private static Logger log = Logger.getLogger(WelcomeMessenger.class);

    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;
    private List<WelcomeMessage> welcomeMessages;
    private final String dataFile = "./data/wm.data";
    private Set<Integer> ignored;

    @Autowired
    public WelcomeMessenger(Query query) {
        this.query = query;
    }

    @PostConstruct
    private void init () {
        welcomeMessages = config.getClasses(WelcomeMessage.class, "wm");
        loadIgnored();
    }

    private void loadIgnored () {
        try (BufferedReader reader = new BufferedReader(new FileReader(dataFile))) {
            Set<Integer> set = new HashSet<>();
            String line; while ((line = reader.readLine()) != null) set.add(Integer.parseInt(line));
            ignored = Collections.synchronizedSet(set);

        } catch (FileNotFoundException e) {}
        catch (IOException e) {
            log.error("Failed to read ignored from file.", e);
        } catch (Exception e) {
            log.fatal("Corrupted ignored file!", e);
        }

        if (ignored == null)
            ignored = new HashSet<>();
    }

    @PreDestroy
    private void save () {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(dataFile)), true)) {
            ignored.forEach(writer::println);
        } catch (IOException e) {
            log.error("Failed to save ignored client list to a file.", e);
        }
    }

    public void sendWelcomeMessageToClient (String eventType, HashMap<String, String> eventInfo) {
        try {
            if (!"1".equals("client_type")) { // Client is not query client
                int clientId = Integer.parseInt(eventInfo.get("clid"));
                Client client = query.getClientInfo(clientId);

                synchronized (ignored) {
                    if (!ignored.contains(client.getDatabaseId())) {
                        for (WelcomeMessage message : welcomeMessages) {
                            if (client.isInServergroup(message.getGroups()))
                                query.sendTextMessageToClient(client.getId(), message.getMessage(client));
                        }
                    }
                }
            }
        } catch (QueryException e) {
            log.error("Failed to send welcome message to client, query returned error: " + e.getMessage());
        }
    }

    @Teamspeak3Command("!ignore")
    public CommandResponse addToIgnored (Client client, String params) {
        ignored.add(client.getDatabaseId());
        return new CommandResponse(config.getString("messages.ignore[@response]"));
    }

    @Teamspeak3Command("!unignore")
    public CommandResponse removeFromIgnored (Client client, String params) {
        ignored.remove(client.getDatabaseId());
        return new CommandResponse(config.getString("messages.unignore[@response]"));
    }
}
