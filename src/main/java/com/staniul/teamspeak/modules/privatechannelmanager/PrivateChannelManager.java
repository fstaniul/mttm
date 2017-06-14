package com.staniul.teamspeak.modules.privatechannelmanager;

import com.staniul.teamspeak.commands.validators.TwoIntegerParamsValidator;
import com.staniul.teamspeak.query.*;
import com.staniul.teamspeak.query.channel.ChannelFlagConstants;
import com.staniul.teamspeak.query.channel.ChannelProperties;
import com.staniul.taskcontroller.Task;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.commands.validators.IntegerParamsValidator;
import com.staniul.teamspeak.commands.validators.ValidateParams;
import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.util.SerializeUtil;
import com.staniul.xmlconfig.UseConfig;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.WireConfig;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Teamspeak 3 module responsible for managing teamspeak 3 private channels.
 */
@Component
@UseConfig("modules/pcm.xml")
public class PrivateChannelManager {
    private static Logger log = Logger.getLogger(PrivateChannelManager.class);

    private final String fileName = ".data/pcm.data";

    @WireConfig
    private CustomXMLConfiguration config;
    private Query query;

    private final Object channelsLock = new Object();
    private List<PrivateChannel> channels;

    @Autowired
    public PrivateChannelManager(Query query) throws ConfigurationException {
        this.query = query;
    }

    @PostConstruct
    private void loadChannels() {
        try {
            channels = SerializeUtil.deserialize(fileName);
        } catch (IOException | ClassNotFoundException e) {
            log.error("Failed to read object from file!", e);
            createChannelsFromTeamspeak3Server();
        }
    }

    @PreDestroy
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
    @ClientGroupAccess(value = "administrators")
    @ValidateParams(IntegerParamsValidator.class)
    public CommandResponse deleteChannelCommand(Client client, String params) throws QueryException {
        int channelNumber = Integer.parseInt(params);
        try {
            if (deleteChannel(channelNumber)) {
                return new CommandResponse(config.getString("messages.delete[@successful]").replace("$NUMBER$", Integer.toString(channelNumber)));
            }
            else
                return new CommandResponse(config.getString("messages.delete[@fail]").replace("$NUMBER$", Integer.toString(channelNumber)));
        } catch (QueryException e) {
            log.error(String.format("Failed to delete channel with number (%d)", channelNumber), e);
            throw e;
        }
    }

    public boolean deleteChannel(int channelNumber) throws QueryException {
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

    private ChannelProperties createDefaultPropertiesForFreeChannel(int channelNumber) {
        String name = String.format("[%03d] %s", channelNumber, config.getString("freechannel[@name]"));
        String description = config.getString("freechannel[@description]").replace("$NUMBER$", Integer.toString(channelNumber));
        return new ChannelProperties()
                .setName(name)
                .setTopic(config.getString("freechannel[@topic]"))
                .setDescription(description)
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
        String name = String.format("[%03d] %s", number, config.getString("clientchannel[@name]").replace("$NICKNAME$", client.getNickname()));
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

    @Task(delay = 300000)
    public void checkChannels() {
        synchronized (channelsLock) {
            boolean stillEmpty = true;

            for (int i = channels.size() - 1; i >= 0; i--) {
                PrivateChannel channel = channels.get(i);

                if (!channel.isFree())
                    stillEmpty = false;

                if (stillEmpty)
                    channels.remove(i);

                if (!channel.isFree()) {
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
    }

    private void checkIfShouldBeMoved(PrivateChannel privateChannel, Channel channel) throws QueryException {
        Pattern pattern = Pattern.compile(".*MOVE ([0-9]*)?.*");
        Matcher matcher = pattern.matcher(channel.getName());
        if (matcher.find()) {
            Integer channelNumber = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : null;
            PrivateChannel freeChannel = null;

            if (channelNumber != null) {
                freeChannel = channels.stream()
                        .filter(ch -> ch.getNumber() == channelNumber)
                        .filter(PrivateChannel::isFree)
                        .findFirst()
                        .orElse(null);
            }

            if (freeChannel == null) {
                freeChannel = getFreeChannel();
            }

            if (freeChannel.getNumber() >= privateChannel.getNumber()) return;

            //Create a free channel in place of client channel.
            ChannelProperties properties = createDefaultPropertiesForFreeChannel(privateChannel.getNumber())
                    .setOrder(privateChannel.getId());
            int newFreeChannelId = query.channelCreate(properties);

            //Move clients channel
            query.channelMove(privateChannel.getId(), freeChannel.getId());

            //Delete free channel
            query.channelDelete(freeChannel.getId());

            //Rename clients channel.
            String newChannelName = String.format("[%03d] %s", freeChannel.getNumber(),
                    config.getString("clientchannel[@movedname]")
                            .replace("$FROM$", Integer.toString(privateChannel.getNumber())));
            query.channelRename(newChannelName, privateChannel.getId());

            //Set client channel data in place of free:
            freeChannel.setId(privateChannel.getId());
            freeChannel.setOwner(privateChannel.getOwner());

            //Update free channel data:
            privateChannel.setId(newFreeChannelId);
            privateChannel.setOwner(PrivateChannel.FREE_CHANNEL_OWNER);
        }
    }

    private void checkChannelName(PrivateChannel privateChannel, Channel channel) throws QueryException {
        String nameTemplate = String.format("\\[%03d\\].*", privateChannel.getNumber());
        if (!channel.getName().matches(nameTemplate)) {
            String newChannelName = String.format("[%03d] %s", privateChannel.getNumber(), config.getString("clientchannel[@invalidnumbername]"));
            query.channelRename(newChannelName, privateChannel.getId());
        }
    }

    @Teamspeak3Command("!chso")
    @ClientGroupAccess("administrators")
    @ValidateParams(TwoIntegerParamsValidator.class)
    public CommandResponse changeChannelOwnerCommand(Client client, String params) throws QueryException {
        String[] splParams = params.split("\\s+");
        int channelNumber = Integer.parseInt(splParams[0]);
        int clientDatabaseId = Integer.parseInt(splParams[1]);

        synchronized (channelsLock) {
            PrivateChannel channel = channels.stream()
                    .filter(ch -> ch.getNumber() == channelNumber)
                    .findFirst()
                    .orElse(null);

            if (channel == null)
                return new CommandResponse(config.getString("messages.chso[@notfound]").replace("$NUMBER$", splParams[0]));

            query.setChannelGroup(clientDatabaseId, channel.getId(), config.getInt("channelgroups.owner[@id]"));
            query.setChannelGroup(channel.getOwner(), channel.getId(), config.getInt("channelgroups.guest[@id]"));
            channel.setOwner(clientDatabaseId);

            return new CommandResponse(config.getString("messages.chso[@success]")
                    .replace("$NUMBER$", Integer.toString(channel.getNumber()))
                    .replace("$CLDBID$", Integer.toString(channel.getOwner())));
        }
    }
}
