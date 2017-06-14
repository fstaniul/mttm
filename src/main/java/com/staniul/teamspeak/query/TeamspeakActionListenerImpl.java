package com.staniul.teamspeak.query;

import com.staniul.teamspeak.TeamspeakCoreController;
import de.stefan1200.jts3serverquery.TeamspeakActionListener;

import java.util.HashMap;

public class TeamspeakActionListenerImpl implements TeamspeakActionListener {
    private TeamspeakCoreController controller;

    public TeamspeakActionListenerImpl(TeamspeakCoreController controller) {
        this.controller = controller;
    }

    @Override
    public void teamspeakActionPerformed(String eventType, HashMap<String, String> eventInfo) {

    }
}
