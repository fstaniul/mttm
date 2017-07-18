package com.staniul.teamspeak.modules.messengers.welcomemessanger;

import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@UseConfig("modules/wm.xml")
public class WelcomeMessenger {
    private static Logger log = LogManager.getLogger(WelcomeMessenger.class);

    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;
    private List<WelcomeMessage> welcomeMessages;
    private final String dataFile = "./data/wm.data";
    private final String messagesFile = "./data/msg/wm.txt";
    private Set<Integer> ignored;

    @Autowired
    public WelcomeMessenger(Query query) {
        this.query = query;
    }

    @PostConstruct
    private void init() {
        readMessagesFromFile();
        log.info("Loaded welcome messages: ");
        welcomeMessages.forEach(log::info);
        loadIgnored();
    }

    private void readMessagesFromFile () {
        log.info("Reading messages from file...");
        Pattern messagePattern = Pattern.compile("\\{(.*)}\\s(.*)");
        File messageFile = new File(messagesFile);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(messageFile), StandardCharsets.UTF_8))) {
            welcomeMessages = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher lineMatcher = messagePattern.matcher(line);
                if (lineMatcher.find()) {
                    Set<Integer> groups = Arrays.stream(lineMatcher.group(1).split(","))
                            .map(Integer::parseInt)
                            .collect(Collectors.toSet());
                    String message = lineMatcher.group(2);

                    welcomeMessages.add(new WelcomeMessage(groups, message));
                }
            }
        } catch (IOException e) {
            log.error("Failed to load welcome messages from file!");
        }
        log.info("Finished reading messages.");
    }

    private void loadIgnored() {
        try (BufferedReader reader = new BufferedReader(new FileReader(dataFile))) {
            Set<Integer> set = new HashSet<>();
            String line;
            while ((line = reader.readLine()) != null) set.add(Integer.parseInt(line));
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
    private void save() {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(dataFile)), true)) {
            ignored.forEach(writer::println);
        } catch (IOException e) {
            log.error("Failed to save ignored client list to a file.", e);
        }
    }

    public void sendWelcomeMessageToClient(HashMap<String, String> eventInfo) {
        try {
            int clientId = Integer.parseInt(eventInfo.get("clid"));
            Client client = query.getClientInfo(clientId);

            if (!client.isQuery()) {
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
    public CommandResponse addToIgnored(Client client, String params) {
        ignored.add(client.getDatabaseId());
        return new CommandResponse(config.getString("commands.ignore[@response]"));
    }

    @Teamspeak3Command("!unignore")
    public CommandResponse removeFromIgnored(Client client, String params) {
        ignored.remove(client.getDatabaseId());
        return new CommandResponse(config.getString("commands.unignore[@response]"));
    }
}
