package com.staniul.teamspeak.modules.privatechannelmanager;

import com.staniul.query.*;
import com.staniul.query.channel.ChannelFlagConstants;
import com.staniul.query.channel.ChannelProperties;
import com.staniul.taskcontroller.Task;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.commands.validators.IntegerParamsValidator;
import com.staniul.teamspeak.commands.validators.ValidateParams;
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
    @ValidateParams(IntegerParamsValidator.class)
    public CommandResponse deleteChannelCommand (Client client, String params) throws QueryException {
        int channelNumber = Integer.parseInt(params);
        try {
            if (deleteChannel(channelNumber)) {
                return new CommandResponse(config.getString("messages.delete[@successful]").replace("$NUMBER$", Integer.toString(channelNumber)));
            } else return new CommandResponse(config.getString("messages.delete[@fail]").replace("$NUMBER$", Integer.toString(channelNumber)));
        } catch (QueryException e) {
            log.error(String.format("Failed to delete channel with number (%d)", channelNumber), e);
            throw e;
        }
    }

    public boolean deleteChannel (int channelNumber) throws QueryException {
        synchronized (channelsLock) {
            if (channelNumber < 0 || channelNumber > channels.size())
                return false;

            PrivateChannel channelToDelete = channels.stream()
                    .filter(privateChannel -> privateChannel.getNumber() == channelNumber)
                    .findFirst()
                    .orElse(null);

            if (channelToDelete == null || channelToDelete.isFree()) return false;

            query.channelDelete(channelToDelete.getId());

            if (channelNumber < channels.size()) {
                ChannelProperties properties = createDefaultPropertiesForFreeChannel(channelNumber)
                        .setOrder(channelToDelete.getId());
                int channelId = query.channelCreate(properties);

                channelToDelete.setOwner(PrivateChannel.FREE_CHANNEL_OWNER);
                channelToDelete.setId(channelId);
            }
            else {
                channels.remove(channelToDelete);
            }

            return true;
        }
    }

    private ChannelProperties createDefaultPropertiesForFreeChannel (int channelNumber) {
        String name = String.format("[%03d] %s", channelNumber, config.getString("freechannel[@name]"));
        String descritpion = config.getString("freechannel[@description]").replace("$NUMBER$", Integer.toString(channelNumber));
        return new ChannelProperties()
                .setName(name)
                .setTopic(config.getString("freechannel[@topic]"))
                .setParent(config.getInt("parentchannel[@id]"))
                .setCodec(4)
                .setCodecQuality(1)
                .setMaxClients(0)
                .setMaxFamilyClients(0);
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

                    int channelNumber = freeChannel == null ? channels.size() + 1 : freeChannel.getNumber();
                    ChannelProperties properties = createDefaultPropertiesForClientsChannel(client, channelNumber);

                    clientsChannel = new PrivateChannel(channelNumber);

                    if (freeChannel != null) {
                        properties.setOrder(freeChannel.getId());
                        query.channelDelete(freeChannel.getId());
                        clientsChannel = freeChannel;
                    }

                    int channelId = query.channelCreate(properties);

                    clientsChannel.setId(channelId);
                    clientsChannel.setOwner(client.getDatabaseId());

                    if (freeChannel == null) channels.add(clientsChannel);
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
            query.sendTextMessageToClient(client.getId(), config.getString("messages.create[@fail]"));
        } catch (QueryException e) {
            log.error("Failed to inform client about fail in channel creation.", e);
        }
    }

    private void messageClientAboutSuccessfulCreation(Client client, PrivateChannel clientsChannel) {
        try {
            String message = config.getString("messages.create[@success]").replace("$NUMBER$", Integer.toString(clientsChannel.getNumber()));
            query.sendTextMessageToClient(client.getId(), message);
        } catch (QueryException e) {
            log.error("Failed to inform client about successful channel creation.", e);
        }
    }

    private ChannelProperties createDefaultPropertiesForClientsChannel(Client client, int number) {
        String name = String.format("[%03d] %s %s", number, config.getString("clientchannel[@name]"), client.getNickname());
        if (name.length() > 40) name = name.substring(0, 40);

        return new ChannelProperties()
                .setName(name)
                .setCodec(4)
                .setCodecQuality(10)
                .setDescription(config.getString("clientchannel[@description]").replace("$NICKNAME$", client.getNickname()))
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

    @Task(delay = 60000)
    public void checkChannels () {
        synchronized (channelsLock) {
            boolean stillEmpty = true;

            for (int i = channels.size() - 1; i >= 0; i--) {
                PrivateChannel channel = channels.get(i);

                if (!channel.isFree())
                    stillEmpty = false;

                if (stillEmpty)
                    channels.remove(i);

                try {
                    Channel info = query.getChannelInfo(channel.getId());
                    checkIfShouldBeMoved(channel, info);
                    checkChannelName(channel, info);
                } catch (QueryException e) {
                    log.error("Failed to manage channels!", e);
                }
            }
        }
    }

    private void checkIfShouldBeMoved(PrivateChannel privateChannel, Channel channel) {

    }

    private void checkChannelName(PrivateChannel privateChannel, Channel channel) throws QueryException {
        String nameTemplate = String.format("\\[%03d\\].*", privateChannel.getNumber());
        if (!channel.getName().matches(nameTemplate)) {
            String newChannelName = String.format("[%03d] %s", privateChannel.getNumber(), config.getString("clientchannel[@invalidnumbername]"));
        }
    }
}
