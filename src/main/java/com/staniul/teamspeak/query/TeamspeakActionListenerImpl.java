package com.staniul.teamspeak.query;

import com.staniul.teamspeak.modules.messengers.welcomemessanger.WelcomeMessenger;
import com.staniul.teamspeak.Teamspeak3CoreController;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import de.stefan1200.jts3serverquery.TeamspeakActionListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
@UseConfig("query.xml")
public class TeamspeakActionListenerImpl implements TeamspeakActionListener {
    private static Logger log = LogManager.getLogger(TeamspeakActionListenerImpl.class);

    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;
    private final Teamspeak3CoreController controller;
    private final WelcomeMessenger welcomeMessenger;

    @Autowired
    TeamspeakActionListenerImpl(Query query, Teamspeak3CoreController controller, WelcomeMessenger welcomeMessenger) throws Exception {
        this.query = query;
        this.controller = controller;
        this.welcomeMessenger = welcomeMessenger;
        query.setTeamspeakActionListener(this);
//        config = ConfigurationLoader.load(TeamspeakActionListenerImpl.class);
    }

    @Override
    public void teamspeakActionPerformed(String eventType, HashMap<String, String> eventInfo) {
        if ("notifytextmessage".equals(eventType) && !"serveradmin".equals(eventInfo.get("invokeruid"))) {
            invokeCommand(eventInfo);
        }

        else if ("notifycliententerview".equals(eventType) && !"serveradmin".equals("client_unique_identifier")) {
            welcomeMessenger.sendWelcomeMessageToClient(eventInfo);
            invokeJoinEvent(eventInfo);
        }

        else if ("notifyclientleftview".equals(eventType)) {
            invokeLeaveEvent(eventInfo);
        }

        else if ("notifyclientmoved".equals(eventType) &&
                eventInfo.get("ctid").equals(config.getString("event-channel[@id]"))) {
            try {
                Client client = query.getClientInfo(Integer.parseInt(eventInfo.get("clid")));
                String msg = config.getString("welcome[@msg]").replace("$NICKNAME$", client.getNickname());
                query.sendTextMessageToClient(client.getId(), msg);
            } catch (QueryException e) {
                log.error("Failed to send message to admin when he joined channel to get message from bot!", e);
            }
        }
    }

    private void invokeLeaveEvent(HashMap<String, String> eventInfo) {
        int clientId = Integer.parseInt(eventInfo.get("clid"));
        controller.callLeaveEvents(clientId);
    }

    private void invokeJoinEvent(HashMap<String, String> eventInfo) {
        int clientId = Integer.parseInt(eventInfo.get("clid"));
        try {
            Client client = query.getClientInfo(clientId);
            controller.callJoinEvents(client);
        } catch (QueryException e) {
            log.error("Failed to get client info while client joined server event was fired!", e);
        }
    }

    private void invokeCommand(HashMap<String, String> eventInfo) {
        try {
            int clientId = Integer.parseInt(eventInfo.get("invokerid"));
            Client client = query.getClientInfo(clientId);
            String msg = eventInfo.get("msg");

            String command, params;
            int splitIndex = msg.indexOf(" ");
            if (splitIndex > 0 && splitIndex + 1 < msg.length()) {
                command = msg.substring(0, splitIndex);
                params = msg.substring(splitIndex + 1);
            }
            else {
                command = msg.trim();
                params = "";
            }

            controller.callCommand(command, client, params);
        } catch (QueryException e) {
            log.error("Failed to get client info when text message event was fired!", e);
        }
    }
}
