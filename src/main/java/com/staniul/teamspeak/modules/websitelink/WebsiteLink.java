package com.staniul.teamspeak.modules.websitelink;

import com.staniul.api.security.AuthController;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.query.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class WebsiteLink {
    private final AuthController authController;

    @Autowired
    public WebsiteLink(AuthController authController) {this.authController = authController;}

    @Teamspeak3Command("!link")
    public CommandResponse sendLinkToConnect (Client client, String params) {
        String token = authController.createTokenForClient(client);

        token = Base64.getEncoder().withoutPadding().encodeToString(token.getBytes(StandardCharsets.UTF_8));

        return new CommandResponse("ts.electronicsports.pl/linkts3.php?token=" + token);
    }
}
