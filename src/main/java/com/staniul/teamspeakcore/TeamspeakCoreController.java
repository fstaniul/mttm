package com.staniul.teamspeakcore;

import com.staniul.configuration.ConfigurationLoader;
import com.staniul.configuration.annotations.ConfigFile;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Controller of teamspeak 3 behaviour, that is events and commands. All commands called on teamspeak 3 server must be
 * registered within this controller to be invoked. All events that should be aware of clients joining and leaving
 * teamspeak 3 server should be registered within this controller.
 */
@Component
@ConfigFile("teamspeakcore.xml")
public class TeamspeakCoreController {
    private XMLConfiguration config;

    @Autowired
    public TeamspeakCoreController() throws ConfigurationException {
        config = ConfigurationLoader.load(TeamspeakCoreController.class);
    }
}
