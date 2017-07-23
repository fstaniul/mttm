package com.staniul.teamspeak.modules.messengers.welcomemessanger;

import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.commands.validators.IntegerParamsValidator;
import com.staniul.teamspeak.commands.validators.TwoIntegerParamsValidator;
import com.staniul.teamspeak.commands.validators.ValidateParams;
import com.staniul.teamspeak.query.Client;
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

    private void readMessagesFromFile() {
        log.info("Reading messages from file...");
        Pattern messagePattern = Pattern.compile("\\{(.*)}\\s(.*)");
        File messageFile = new File(messagesFile);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(messageFile), StandardCharsets.UTF_8))) {
            welcomeMessages = Collections.synchronizedList(new ArrayList<>());
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

        } catch (FileNotFoundException e) {} catch (IOException e) {
            log.error("Failed to read ignored from file.", e);
        } catch (Exception e) {
            log.fatal("Corrupted ignored file!", e);
        }

        if (ignored == null)
            ignored = new HashSet<>();
    }

    @PreDestroy
    private void save() {
        synchronized (welcomeMessages) {
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(dataFile)), true)) {
                ignored.forEach(writer::println);
            } catch (IOException e) {
                log.error("Failed to save ignored client list to a file.", e);
            }
        }
    }

    public void sendWelcomeMessageToClient(HashMap<String, String> eventInfo) {
        try {
            int clientId = Integer.parseInt(eventInfo.get("clid"));
            Client client = query.getClientInfo(clientId);

            if (!client.isQuery()) {
                synchronized (ignored) {
                    synchronized (welcomeMessages) {
                        if (!ignored.contains(client.getDatabaseId())) {
                            for (WelcomeMessage message : welcomeMessages) {
                                if (client.isInServergroup(message.getGroups()))
                                    query.sendTextMessageToClient(client.getId(), message.getMessage(client));
                            }
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

    @Teamspeak3Command("!wmshow")
    @ClientGroupAccess("servergroups.headadmins")
    public CommandResponse showWelcomeMessages(Client client, String params) {
        StringBuilder sb = new StringBuilder();

        if (welcomeMessages.size() > 0) {
            sb.append(config.getString("commands.wmshow[@header]")).append("\n");
            synchronized (welcomeMessages) {
                for (int i = 0; i < welcomeMessages.size(); i++) {
                    WelcomeMessage wm = welcomeMessages.get(i);
                    sb.append(i)
                            .append(": {").append(wm.getGroups().toString()).append("} ")
                            .append(wm.getMessage()).append("\n");
                }
            }
        }
        else {
            sb.append(config.getString("commands.wmshow[@nomsg]"));
        }
        return new CommandResponse(sb.toString());
    }

    @Teamspeak3Command("!wmadd")
    @ClientGroupAccess("servergroups.headadmins")
    @ValidateParams(WelcomeMessageValidator.class)
    public CommandResponse addWelcomeMessage(Client client, String params) {
        Pattern messagePattern = WelcomeMessageValidator.getPattern();
        Matcher matcher = messagePattern.matcher(params);

        if (matcher.find()) {
            Set<Integer> groups = Arrays.stream(matcher.group(1).split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet());
            String message = matcher.group(4);

            WelcomeMessage wm = new WelcomeMessage(groups, message);
            welcomeMessages.add(wm);

            String responseMessage = config.getString("commands.wmadd[@response]")
                    .replace("$GROUPS$", groups.toString().replace("\\s+", ""))
                    .replace("$MESSAGE$", message);

            return new CommandResponse(responseMessage);
        }

        return new CommandResponse("Error in validating parameters!");
    }

    @Teamspeak3Command("!wmswap")
    @ClientGroupAccess("servergroups.headadmins")
    @ValidateParams(TwoIntegerParamsValidator.class)
    public CommandResponse swapWelcomeMessages(Client client, String params) {
            Matcher matcher = TwoIntegerParamsValidator.getPattern().matcher(params);
            int message1 = Integer.parseInt(matcher.group(1));
            int message2 = Integer.parseInt(matcher.group(2));

            if (message1 < 0 || message1 >= welcomeMessages.size() || message2 < 0 || message2 >= welcomeMessages.size()) {
                return new CommandResponse(config.getString("commands.wmswap[@outofrange]"));
            }

        synchronized (welcomeMessages) {
            WelcomeMessage tmp = welcomeMessages.get(message1);
            welcomeMessages.set(message1, welcomeMessages.get(message2));
            welcomeMessages.set(message2, tmp);
        }

        return new CommandResponse(config.getString("commands.wmswap[@response]"));
    }

    @Teamspeak3Command("!wmdel")
    @ClientGroupAccess("servergroups.headadmins")
    @ValidateParams(IntegerParamsValidator.class)
    public CommandResponse deleteWelcomeMessage(Client client, String params) {
        int msg = Integer.parseInt(params);

        if (msg < 0 || msg >= welcomeMessages.size())
            return new CommandResponse(config.getString("commands.wmdel[@outofrange]"));

        WelcomeMessage message = welcomeMessages.get(msg);

        welcomeMessages.remove(msg);

        String response = config.getString("commands.wmdel[@response]")
                .replace("$ID$", Integer.toString(msg))
                .replace("$MESSAGE$", message.toString());

        return new CommandResponse(response);
    }
}
