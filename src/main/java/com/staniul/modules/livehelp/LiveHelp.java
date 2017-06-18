package com.staniul.modules.livehelp;

import com.staniul.taskcontroller.Task;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@UseConfig("modules/lh.xml")
public class LiveHelp {
    private static Logger log = Logger.getLogger(LiveHelp.class);

    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;
    private final Object clientsLock = new Object();
    private List<Client> clientsCalled;

    public LiveHelp(Query query) {
        this.query = query;
        clientsCalled = new LinkedList<>();
    }

    @Task(delay = 5 * 1000)
    public void checkForClients () throws QueryException {
        synchronized (clientsLock) {
            List<Client> clientList = query.getClientList();

            List<Client> clientsInHelp = clientList.stream()
                    .filter(c -> c.getCurrentChannelId() == config.getInt("channel[@id]"))
                    .filter(c -> !c.isInServergroup(config.getIntSet("client-groups[@ignore]")))
                    .collect(Collectors.toList());
            clientsInHelp.removeIf(c -> clientsCalled.contains(c));
            clientsCalled.addAll(clientsInHelp);

            List<Client> admins = clientList.stream()
                    .filter(c -> c.isInServergroup(config.getIntSet("admin-groups[@ids]")))
                    .filter(c -> !c.isInServergroup(config.getIntSet("admin-groups[@ignore]")))
                    .collect(Collectors.toList());

            List<Client> adminsWhoCanHelp = admins.stream()
                    .filter(c -> !c.isAway() && c.getHeadphones().isConnected() && !c.getHeadphones().isMuted())
                    .collect(Collectors.toList());

            String message;
            if (adminsWhoCanHelp.size() > 0)
                message = config.getString("messages.client[@info]").replace("$COUNT$", Integer.toString(adminsWhoCanHelp.size()));
            else message = config.getString("messages.client[@no-admins-online]");

            for (Client clientInNeed : clientsInHelp) {
                String pokeMsg = config.getString("messages.admin[@poke]").replace("$NICKNAME$", clientInNeed.getNickname());
                for (Client admin : adminsWhoCanHelp)
                    query.pokeClient(admin.getId(), pokeMsg, false);

                String url = String.format("[URL=client://%d/%s][B]%s[/B][/URL]", clientInNeed.getId(), clientInNeed.getUniqueId(), clientInNeed.getNickname());
                String msg = config.getString("messages.admin[@info]").replace("$URL$", url);
                for (Client admin : admins)
                    query.sendTextMessageToClient(admin.getId(), msg);

                query.sendTextMessageToClient(clientInNeed.getId(), message);
            }
        }
    }

    @Task(delay = 5 * 60 * 1000)
    public void clearClientsList () {
        synchronized (clientsLock) {
            clientsCalled.clear();
        }
    }
}
