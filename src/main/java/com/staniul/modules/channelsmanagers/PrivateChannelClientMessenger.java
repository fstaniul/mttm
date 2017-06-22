package com.staniul.modules.channelsmanagers;

import com.staniul.teamspeak.events.EventType;
import com.staniul.teamspeak.events.Teamspeak3Event;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Component
@UseConfig("modules/pcm.xml")
public class PrivateChannelClientMessenger {
    private static Logger log = LogManager.getLogger(PrivateChannelClientMessenger.class);

    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;
    private final String dataFile = "./data/pccm.data";
    private final String delimiter = ";;;";
    private final Object messageLock = new Object();
    private Map<Integer, String> messageMap;

    @Autowired
    public PrivateChannelClientMessenger(Query query) {
        this.query = query;
    }

    @PostConstruct
    private void init() {
        synchronized (messageLock) {
            log.info("Loading private channel manager client messages from data file...");
            File file = new File(dataFile);
            if (file.exists() && file.isFile()) {
                log.info("File exists, reading...");
                Map<Integer, String> map = new HashMap<>();
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] split = line.split(delimiter);
                        map.put(Integer.parseInt(split[0]), split[1]);
                    }

                    messageMap = map;
                } catch (IOException e) {
                    log.error("Failed to load private channel manager client messages from file. Will be empty.", e);
                    messageMap = new HashMap<>();
                }
            }
            else {
                log.info("File with data does not exits. Messages will be empty.");
                messageMap = new HashMap<>();
            }
        }
    }

    @PreDestroy
    private void save() {
        synchronized (messageLock) {
            log.info("Saving private channel manager client messages to file...");
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(dataFile)), true)) {
                for (Map.Entry<Integer, String> entry : messageMap.entrySet()) {
                    writer.printf("%d%s%s\n", entry.getKey(), delimiter, entry.getValue());
                }
                log.info("Saved messages to file.");
            } catch (IOException ex) {
                log.error("Failed to save messages to file.", ex);
                log.info("Saving messages here:");
                messageMap.forEach((key, value) -> System.out.printf("%d%s%s\n", key, delimiter, value));
            }
        }
    }

    @Teamspeak3Event(EventType.JOIN)
    public void sendMessagesToClientOnJoin (Client client) throws QueryException {
        synchronized (messageLock) {
            if (messageMap.containsKey(client.getDatabaseId())) {
                query.sendTextMessageToClient(client.getId(), messageMap.get(client.getDatabaseId()));
                messageMap.remove(client.getDatabaseId());
            }
        }
    }

    public void addMessage (Integer clientDatabaseId, String message) {
        synchronized (messageLock) {
            try {
                Client client = query.getClientList().stream()
                        .filter(c -> c.getDatabaseId() == clientDatabaseId)
                        .findFirst()
                        .orElse(null);

                //Client is online so we can send him message!
                if (client != null) {
                    query.sendTextMessageToChannel(client.getId(), message);
                }

                //Client is offline so we dont send message, we store it and we will send it later.
                else {
                    messageMap.put(clientDatabaseId, message);
                }
            } catch (QueryException e) {
                log.error(String.format("Failed to send message to client (%d) about his channel.", clientDatabaseId), e);
            }
        }
    }
}
