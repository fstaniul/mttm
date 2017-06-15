package com.staniul.teamspeak.query;

import com.staniul.teamspeak.TeamspeakCoreController;
import de.stefan1200.jts3serverquery.TeamspeakActionListener;
import org.apache.log4j.Logger;

import java.util.HashMap;

public class TeamspeakActionListenerImpl implements TeamspeakActionListener {
    private static Logger log = Logger.getLogger(TeamspeakActionListenerImpl.class);

    private TeamspeakCoreController controller;
    private Query query;

    TeamspeakActionListenerImpl(Query query, TeamspeakCoreController controller) {
        this.query = query;
        this.controller = controller;
    }

    @Override
    public void teamspeakActionPerformed(String eventType, HashMap<String, String> eventInfo) {
        if ("notifytextmessage".equals(eventType) && !"serveradmin".equals(eventInfo.get("invokeruid"))) {
            invokeCommand(eventInfo);
        }

        else if ("notifycliententerview".equals(eventType) && !"serveradmin".equals("client_unique_identifier")) {
            invokeJoinEvent(eventInfo);
        }

        else if ("notifyclientleftview".equals(eventType)) {
            invokeLeaveEvent(eventInfo);
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
            if (splitIndex > 0 || splitIndex + 1 < msg.length()) {
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
