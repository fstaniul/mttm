package com.staniul.modules.adminlists;

import com.staniul.security.clientaccesscheck.ClientGroupAccess;

import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.events.EventType;
import com.staniul.teamspeak.events.Teamspeak3Event;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@UseConfig("modules/aol.xml")
public class AdminOnlineList {
    private static Logger log = Logger.getLogger(AdminOnlineList.class);

    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;
    private List<Servergroup> servergroupList;
    private final Object adminsLock = new Object();
    private List<Admin> admins;

    @Autowired
    public AdminOnlineList(Query query) {
        this.query = query;
        admins = new ArrayList<>();
    }

    @PostConstruct
    private void init() throws QueryException {
        synchronized (adminsLock) {
            setServergroupList();
            setCurrentlyOnlineAdmins();
        }
    }

    private void setServergroupList () {
        servergroupList = config.getClasses(Servergroup.class, "groups.servergroup");
    }

    private void sortAdmins () {
        admins.sort(Comparator.comparingInt(Admin::getRank).thenComparing(Admin::getNickname));
    }

    @Teamspeak3Event(EventType.JOIN)
    @ClientGroupAccess("servergroups.admins")
    public void adminJoinedEvent (Client client) {
        synchronized (adminsLock) {
            addAdminToList(client);
            refreshDisplay();
        }
    }

    private void addAdminToList (Client client) {
        Admin admin = createAdmin(client);
        admins.add(admin);
        sortAdmins();
    }

    private Admin createAdmin(Client client) {
        for (Servergroup servergroup : servergroupList) {
            if (client.isInServergroup(servergroup.getId()))
                return new Admin(client, servergroup.getRank(), servergroup.getIcon());
        }

        throw new IllegalArgumentException("Client is not an administrator!");
    }

    @Teamspeak3Event(EventType.LEAVE)
    public void removeClientFromList (Integer id) {
        synchronized (adminsLock) {
            int size = admins.size();
            admins.removeIf(admin -> admin.getId() == id);
            if (admins.size() != size) {
                sortAdmins();
                refreshDisplay();
            }
        }
    }

    private void refreshDisplay() {
        StringBuilder sb = new StringBuilder("[CENTER][B][SIZE=10]");
        for (Admin admin : admins) {
            sb.append("[IMG]")
                    .append(admin.getIcon())
                    .append("[/IMG] [URL=client://")
                    .append(admin.getId())
                    .append("/")
                    .append(admin.getUniqueId())
                    .append("]")
                    .append(admin.getNickname())
                    .append("[/URL]\n\n");
        }

        sb.append("[/SIZE][/B][/CENTER]");

        try {
            query.channelChangeDescription(sb.toString(), config.getInt("displaychannel[@id]"));
        } catch (QueryException e) {
            log.error(String.format("Failed to change channel description of channel (%d)", config.getInt("displaychannel[@id]")));
        }
    }

    public void setCurrentlyOnlineAdmins() throws QueryException {
        Set<Integer> adminGroups = servergroupList.stream().map(Servergroup::getId).collect(Collectors.toSet());
        List<Client> clients = query.getClientList().stream().filter(c -> c.isInServergroup(adminGroups)).collect(Collectors.toList());
        clients.forEach(this::addAdminToList);
        refreshDisplay();
    }

    @Teamspeak3Command("!refaol")
    @ClientGroupAccess("servergroup.admins")
    public CommandResponse refreshAdminOnlineList (Client client, String params) throws QueryException {
        synchronized (adminsLock) {
            admins = new ArrayList<>();
            setCurrentlyOnlineAdmins();
            refreshDisplay();
            return new CommandResponse(config.getString("messages.refaol[@response]"));
        }
    }
}
