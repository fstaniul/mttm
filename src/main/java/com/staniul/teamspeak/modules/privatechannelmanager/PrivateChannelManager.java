package com.staniul.teamspeak.modules.privatechannelmanager;

import com.staniul.query.*;
import com.staniul.query.channel.ChannelFlagConstants;
import com.staniul.query.channel.ChannelProperties;
import com.staniul.taskcontroller.Task;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.teamspeak.security.clientaccesscheck.ClientServergroupAccessCheck;
import com.staniul.util.SerializeUtil;
import com.staniul.xmlconfig.ConfigFile;
import com.staniul.xmlconfig.ConfigurationLoader;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@ConfigFile("modules/pcm.xml")
public class PrivateChannelManager {
    private static Logger log = Logger.getLogger(PrivateChannelManager.class);

    private final String fileName = ".data/pcm.data";

    private CustomXMLConfiguration config;
    private Query query;

    private final Object channelsLock = new Object();
    private List<PrivateChannel> channels;

    @Autowired
    public PrivateChannelManager(Query query) throws ConfigurationException {
        config = ConfigurationLoader.load(PrivateChannelManager.class);
        this.query = query;
        loadChannels();
    }

    private void loadChannels() {
        try {
            channels = SerializeUtil.deserialize(fileName);
        } catch (IOException | ClassNotFoundException e) {
            log.error("Failed to read object from file!", e);
            createChannelsFromTeamspeak3Server();
        }
    }

    private void saveChannels() {
        try {
            SerializeUtil.serialize(fileName, channels);
        } catch (IOException e) {
            log.error("Failed to serialize channels data.", e);
        }
    }

    private void createChannelsFromTeamspeak3Server() {
        try {
            int parentChannelId = config.getInt("parentchannel[@id]");
            int ownerChannelgroup = config.getInt("channelgroups.owner[@id]");

            List<Channel> teamspeak3PrivateChannels = query.getChannelList()
                    .stream()
                    .filter(c -> c.getParentId() == parentChannelId)
                    .collect(Collectors.toList());

            channels = new ArrayList<>();
            for (int i = 0; i < teamspeak3PrivateChannels.size(); i++) {
                Channel channel = teamspeak3PrivateChannels.get(i);
                List<ClientChannelInfo> ccil = query.getChannelgroupClientList(channel.getId())
                        .stream()
                        .filter(cci -> cci.getChannelgroupId() == ownerChannelgroup)
                        .collect(Collectors.toList());

                channels.add(new PrivateChannel(channel.getId(), i + 1,
                        ccil.size() > 0 ? ccil.get(ccil.size() - 1).getClientDatabaseId() : -1));
            }
        } catch (QueryException e) {
            log.error("Failed to create channels list from teamspeak 3 server.");
        }
    }

    @Teamspeak3Command("!chdel")
    @ClientGroupAccess(value = "administrators", check = ClientServergroupAccessCheck.class)
    public CommandResponse deleteChannel (Client client, String params) {

    }

    @Task(delay = 5000)
    public void checkForClientsTask() {
        try {
            int channelId = config.getInt("eventChannelId[@id]");
            List<Client> clients = query.getClientList().stream().filter(client -> client.getCurrentChannelId() == channelId).collect(Collectors.toList());
            clients.forEach(this::createChannelForClient);
        } catch (QueryException e) {
            log.error("Failed to create channel for clients!", e);
        }
    }

    private void createChannelForClient(Client client) {
        synchronized (channelsLock) {
            try {
                PrivateChannel clientsChannel = getClientsChannel(client);

                if (clientsChannel == null) {
                    PrivateChannel freeChannel = getFreeChannel();

                    if (freeChannel == null)
                        clientsChannel = createNewChannelForClient(client);

                    else clientsChannel = changeFreeChannelToBeClientsOne(client, freeChannel);
                }

                moveClientToChannel(client, clientsChannel);
                messageClientAboutSuccessfulCreation(client, clientsChannel);
            } catch (QueryException e) {
                log.error("Failed to create channel for client!", e);
                messageClientAboutFailedCreation(client);
            }
        }
    }

    private void messageClientAboutFailedCreation(Client client) {
        try {
            query.sendTextMessageToClient(client.getId(), config.getString("messages[@fail]"));
        } catch (QueryException e) {
            log.error("Failed to inform client about fail in channel creation.", e);
        }
    }

    private void messageClientAboutSuccessfulCreation(Client client, PrivateChannel clientsChannel) {
        try {
            String message = config.getString("messages[@success]").replace("$NUMBER$", Integer.toString(clientsChannel.getNumber()));
            query.sendTextMessageToClient(client.getId(), message);
        } catch (QueryException e) {
            log.error("Failed to inform client about successful channel creation.", e);
        }
    }

    private PrivateChannel createNewChannelForClient(Client client) throws QueryException {
        int number = channels.size() + 1;
        ChannelProperties properties = createDefaultPropertiesForClientsChannel(client, number);

        int channelId = query.channelCreate(properties);

        PrivateChannel clientsChannel = new PrivateChannel(channelId, number, client.getDatabaseId());
        channels.add(clientsChannel);
        return clientsChannel;
    }

    private PrivateChannel changeFreeChannelToBeClientsOne(Client client, PrivateChannel freeChannel) throws QueryException {
        ChannelProperties properties = createDefaultPropertiesForClientsChannel(client, freeChannel.getNumber());
        properties.setOrder(freeChannel.getId());

        int channelId = query.channelCreate(properties);
        query.channelDelete(freeChannel.getId());

        freeChannel.setId(channelId);
        freeChannel.setOwner(client.getDatabaseId());

        return freeChannel;
    }

    private ChannelProperties createDefaultPropertiesForClientsChannel(Client client, int number) {
        String name = String.format("[%02d] %s %s", number, config.getString("clientchannel[@name]"), client.getNickname());
        if (name.length() > 40) name = name.substring(0, 40);

        return new ChannelProperties()
                .setName(name)
                .setCodec(4)
                .setCodecQuality(10)
                .setDescription(String.format(config.getString("clientchannel[@description]"), client.getNickname()))
                .setParent(config.getInt("parentchannel[@id]"))
                .setFlag(ChannelFlagConstants.MAXCLIENTS_UNLIMITED | ChannelFlagConstants.MAXFAMILYCLIENTS_UNLIMITED);
    }

    private PrivateChannel getFreeChannel() {
        return channels.stream()
                .filter(PrivateChannel::isFree)
                .findFirst()
                .orElse(null);
    }

    private PrivateChannel getClientsChannel(Client client) {
        return channels.stream()
                .filter(pc -> pc.getOwner() == client.getDatabaseId())
                .findFirst()
                .orElse(null);
    }

    private void moveClientToChannel(Client client, PrivateChannel clientsChannel) {
        try {
            query.moveClient(client.getId(), clientsChannel.getId());
        } catch (QueryException e) {
            log.error(e.getMessage(), e);
        }
    }
}
