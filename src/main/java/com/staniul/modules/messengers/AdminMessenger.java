package com.staniul.modules.messengers;

import com.staniul.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.commands.validators.ValidateParams;
import com.staniul.teamspeak.events.EventType;
import com.staniul.teamspeak.events.Teamspeak3Event;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@UseConfig("modules/amsg.xml")
public class AdminMessenger {
    private static Logger log = Logger.getLogger(AdminMessenger.class);

    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;
    private final String dataFile = "./data/amsg.data";
    private final String delimiter = ";;;";
    private final Object messageLock = new Object();
    private List<Message> messages;

    @Autowired
    public AdminMessenger(Query query) {
        this.query = query;
    }

    @PostConstruct
    private void init() {
        log.info("Loading admin messages...");
        File dataFile = new File(this.dataFile);
        if (dataFile.exists() && dataFile.isFile()) {
            log.info("File exists, loading from file...");
            try (BufferedReader reader = new BufferedReader(new FileReader(dataFile))) {
                String line;
                List<Message> msg = new ArrayList<>();
                while ((line = reader.readLine()) != null) {
                    String[] split = line.split(delimiter);
                    msg.add(new Message(split[0], JTS3ServerQuery.decodeTS3String(split[1])));
                }
                messages = new ArrayList<>();
            } catch (IOException e) {
                log.error("Failed to load messages from file! Messages are cleaned.", e);
                messages = new ArrayList<>();
            }
        }
        else {
            messages = new ArrayList<>();
        }
    }

    @PreDestroy
    private void save () {
        log.info("Saving admin messages to file.");
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(dataFile)), true)) {
            for (Message message : messages)
                writer.printf("%s%s%s\n", message.getOwner(), delimiter, JTS3ServerQuery.encodeTS3String(message.getMessage()));
        } catch (IOException e) {
            log.error("Failed to save messages to file", e);
            log.info("Dumping admin messages here: ");
            messages.forEach(m -> System.out.printf("%s%s%s\n", m.getOwner(), delimiter, JTS3ServerQuery.encodeTS3String(m.getMessage())));
        }
    }


    @Teamspeak3Command("!msgadd")
    @ClientGroupAccess("servergroups.admins")
    @ValidateParams(NotEmptyParamsValidator.class)
    public CommandResponse addMessage(Client client, String params) throws QueryException {
        synchronized (messageLock) {
            Message message = new Message(params, client.getNickname());
            messages.add(message);
            sendMessageToAllAdmins(message);
            log.info(String.format("Added new admin message: %s", message));
            return new CommandResponse();
        }
    }

    public void sendMessageToAllAdmins(Message message) throws QueryException {
        Set<Integer> adminGroupIds = config.getIntSet("admin-groups[@ids]");
        List<Client> admins = query.getClientList().stream().filter(c -> c.isInServergroup(adminGroupIds)).collect(Collectors.toList());
        for (Client admin : admins)
            query.sendTextMessageToClient(admin.getId(), message.toString());
    }

    @Teamspeak3Event(EventType.JOIN)
    @ClientGroupAccess("servergroups.admins")
    public void sendMessagesToAdminAfterHeJoinedServer(Client client) throws QueryException {
        synchronized (messageLock) {
            if (messages.size() > 0) {
                query.sendTextMessageToClient(client.getId(),
                        config.getString("welcome-msg[@header]").replace("$COUNT$", Integer.toString(messages.size())));
            }
            for (int i = 0; i < messages.size(); i++) {
                Message message = messages.get(i);
                query.sendTextMessageToClient(client.getId(), i + ": " + message.toString());
            }
        }
    }

    @Teamspeak3Command("!msgdel")
    @ClientGroupAccess("servergroups.admins")
    @ValidateParams(NotEmptyParamsValidator.class)
    public CommandResponse removeMessage(Client client, String params) {
        synchronized (messageLock) {
            Message delMessage = null;
            if (params.matches("\\d+")) {
                int msgId = Integer.parseInt(params);
                if (msgId > 0 && msgId < messages.size())
                    delMessage = messages.get(msgId);
            }
            else {
                String plc = params.toLowerCase();
                delMessage = messages.stream()
                        .filter(m -> m.getMessage().toLowerCase().startsWith(plc))
                        .findFirst()
                        .orElse(null);
            }

            String retMsg;
            if (delMessage != null) {
                messages.remove(delMessage);
                retMsg = config.getString("commands.msgdel[@response]").replace("$MSG$", delMessage.toString());
            }
            else {
                retMsg = config.getString("commands.msgdel[@not-found]");
            }

            return new CommandResponse(retMsg);
        }
    }

    @Teamspeak3Command("!msgshow")
    @ClientGroupAccess("servergroups.admins")
    public CommandResponse showCommands (Client client, String params) {
        synchronized (messageLock) {
            List<String> returnMsg = new LinkedList<>();

            if (this.messages.size() == 0)
                return new CommandResponse(config.getString("commands.msgshow[@no-msg]"));

            for (int i = 0; i < messages.size(); i++) {
                Message message = messages.get(i);
                returnMsg.add(i + ": " + message.toString());
            }

            return new CommandResponse(returnMsg.toArray(new String[]{}));
        }
    }
}
