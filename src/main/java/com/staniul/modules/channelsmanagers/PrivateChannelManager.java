package com.staniul.modules.channelsmanagers;

import com.staniul.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.taskcontroller.Task;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.commands.validators.IntegerParamsValidator;
import com.staniul.teamspeak.commands.validators.TwoIntegerParamsValidator;
import com.staniul.teamspeak.commands.validators.ValidateParams;
import com.staniul.teamspeak.query.*;
import com.staniul.teamspeak.query.channel.ChannelFlagConstants;
import com.staniul.teamspeak.query.channel.ChannelProperties;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Teamspeak 3 module responsible for managing teamspeak 3 private channels.
 */
@Component
@UseConfig("modules/pcm.xml")
public class PrivateChannelManager {
    private static Logger log = LogManager.getLogger(PrivateChannelManager.class);

    private final String fileName = "./data/pcm.data";

    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;
    private final PrivateChannelClientMessenger messenger;

    private final Object channelsLock = new Object();
    private List<PrivateChannel> channels;

    @Autowired
    public PrivateChannelManager(Query query, PrivateChannelClientMessenger messenger) {
        this.query = query;
        this.messenger = messenger;
    }

    @PostConstruct
    private void loadChannels() throws QueryException {
        log.info("Loading private channels data...");
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            log.info("File exists starting to load data.");
            List<PrivateChannel> list = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] split = line.split("\\s+");
                    int number = Integer.parseInt(split[0]);
                    int id = Integer.parseInt(split[1]);
                    int owner = Integer.parseInt(split[2]);

                    list.add(new PrivateChannel(id, number, owner));
                }
                channels = list;
                log.info(String.format("Loaded private channels: %s", channels));
                log.info("Finished loading private channels data from files.");
            } catch (IOException e) {
                log.error("Failed to load private channels data from file, falling back to creating list from teamspeak 3 server.");
                createChannelsFromTeamspeak3Server();
            } catch (Exception e) {
                log.error("Corrupted private channels data file, falling back to creating list from teamspeak 3 server.");
                createChannelsFromTeamspeak3Server();
            }
        }
        else createChannelsFromTeamspeak3Server();
    }

    @PreDestroy
    private void saveChannels() {
        log.info("Saving private channels data.");
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName)), true)) {
            for (PrivateChannel channel : channels)
                writer.printf("%d %d %d\n", channel.getNumber(), channel.getId(), channel.getOwner());
            log.info("Saved private channels data to file.");
        } catch (IOException e) {
            log.error("Failed to save channels data to file, duping content here:\n");
            channels.forEach(c -> System.out.printf("%d %d %d\n", c.getNumber(), c.getId(), c.getOwner()));
        }
    }

    private void createChannelsFromTeamspeak3Server() throws QueryException {
        log.info("Reading private channels data from teamspeak 3 server.");
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
            log.info(String.format("Read channels from teamspeak 3 server: %s", channels));
        } catch (QueryException e) {
            log.fatal("Failed to create channels list from teamspeak 3 server, cannot proceed!", e);
            throw e;
        }
    }

    @Teamspeak3Command("!chdel")
    @ClientGroupAccess(value = "servergroups.admins")
    @ValidateParams(IntegerParamsValidator.class)
    public CommandResponse deleteChannelCommand(Client client, String params) throws QueryException {
        int channelNumber = Integer.parseInt(params);
        try {
            if (deleteChannel(channelNumber, client)) {
                log.info(String.format("Deleted private channel (%d) on clients command (%d, %s)", channelNumber, client.getDatabaseId(), client.getNickname()));
                return new CommandResponse(config.getString("messages.delete[@success]").replace("$NUMBER$", Integer.toString(channelNumber)));
            }
            else
                return new CommandResponse(config.getString("messages.delete[@fail]").replace("$NUMBER$", Integer.toString(channelNumber)));
        } catch (QueryException e) {
            log.error(String.format("Failed to delete channel with number (%d)", channelNumber), e);
            throw e;
        }
    }

    private boolean deleteChannel(int channelNumber, Client admin) throws QueryException {
        synchronized (channelsLock) {
            if (channelNumber < 0 || channelNumber > channels.size())
                return false;

            PrivateChannel channelToDelete = channels.stream()
                    .filter(privateChannel -> privateChannel.getNumber() == channelNumber)
                    .findFirst()
                    .orElse(null);

            if (channelToDelete == null || channelToDelete.isFree()) return false;

            messenger.addMessage(channelToDelete.getOwner(), config.getString("messages.deleted[@by-admin]").replace("$NICKNAME$", admin.getNickname()));

            if (channelNumber < channels.size()) {
                ChannelProperties properties = createDefaultPropertiesForFreeChannel(channelNumber)
                        .setOrder(channelToDelete.getId());
                int channelId = query.channelCreate(properties);

                query.channelDelete(channelToDelete.getId());

                channelToDelete.setOwner(PrivateChannel.FREE_CHANNEL_OWNER);
                channelToDelete.setId(channelId);
            }
            else {
                query.channelDelete(channelToDelete.getId());
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
            int channelId = config.getInt("eventchannel[@id]");
            List<Client> clients = query.getClientList().stream()
                    .filter(client -> client.getCurrentChannelId() == channelId)
                    .filter(client -> !client.isInServergroup(config.getIntSet("servergroups[@ignore]")))
                    .collect(Collectors.toList());
            clients.forEach(this::createChannelForClient);
        } catch (QueryException e) {
            log.error("Failed to create channel for clients!", e);
        }
    }

    private void createChannelForClient(Client client) {
        synchronized (channelsLock) {
            log.info(String.format("Creating channel for client (%d, %s)", client.getDatabaseId(), client.getNickname()));
            try {
                PrivateChannel clientsChannel = getClientsChannel(client);

                if (clientsChannel == null) {
                    PrivateChannel freeChannel = getFreeChannel();

                    int channelNumber = freeChannel == null ? channels.size() + 1 : freeChannel.getNumber();
                    ChannelProperties properties = createDefaultPropertiesForClientsChannel(client, channelNumber);

                    clientsChannel = new PrivateChannel(channelNumber);

                    if (freeChannel != null) {
                        properties.setOrder(freeChannel.getId());
                        clientsChannel = freeChannel;
                    }

                    int channelId = query.channelCreate(properties);
                    createSubChannels(channelId);
                    query.setChannelGroup(client.getDatabaseId(), channelId, config.getInt("channelgroups.owner[@id]"));

                    if (freeChannel != null)
                        query.channelDelete(freeChannel.getId());

                    clientsChannel.setId(channelId);
                    clientsChannel.setOwner(client.getDatabaseId());

                    if (freeChannel == null) channels.add(clientsChannel);
                }

                moveClientToChannel(client, clientsChannel);
                messageClientAboutSuccessfulCreation(client, clientsChannel);
                log.info(String.format("Created channel for client (%d, %s)", client.getDatabaseId(), client.getNickname()));
            } catch (QueryException e) {
                log.error(String.format("Failed to create channel for client (%d, %s)!", client.getDatabaseId(), client.getNickname()), e);
                messageClientAboutFailedCreation(client);
            }
        }
    }

    private void createSubChannels(int channelId) throws QueryException {
        ChannelProperties properties = new ChannelProperties()
                .setCodec(4)
                .setCodecQuality(10)
                .setParent(channelId)
                .setFlag(ChannelFlagConstants.MAXCLIENTS_UNLIMITED | ChannelFlagConstants.MAXFAMILYCLIENTS_UNLIMITED)
                .setName("#1");
        query.channelCreate(properties);
        query.channelCreate(properties.setName("#2"));
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

    @Task(delay = 5 * 60 * 1000)
    public void checkChannels() {
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
                    if (!channel.isFree())
                        checkIfShouldBeMoved(channel, info);
                    checkChannelName(channel, info);

                    if (!channel.isFree())
                        checkIfShouldBeDeleted(channel, info);
                } catch (QueryException e) {
                    log.error("Failed to manage channels! " + e.getMessage(), e);
                }
            }
        }
    }

    private void checkIfShouldBeDeleted(PrivateChannel channel, Channel info) {
        try {
            long emptyForDeletion = TimeUnit.DAYS.toSeconds(config.getLong("clientchannel[@max-empty-days]"));
            if (info.getSecondsEmpty() > emptyForDeletion) {
                ChannelProperties freeChannelProperties = createDefaultPropertiesForFreeChannel(channel.getNumber());
                freeChannelProperties.setOrder(channel.getId());

                int freeChannelId = query.channelCreate(freeChannelProperties);
                query.channelDelete(channel.getId());

                messenger.addMessage(channel.getOwner(), config.getString("messages.deleted[@msg]").replace("$DAYS$", config.getString("clientchannel[@max-empty-days]")));

                channel.setId(freeChannelId);
                channel.setOwner(PrivateChannel.FREE_CHANNEL_OWNER);
            }
        } catch (QueryException e) {
            log.error("Failed to delete clients channel and make free channel in its place.", e);
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
            String newChannelName = String.format("[%03d]%s", freeChannel.getNumber(),
                    channel.getName().substring(5));
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
            String newChannelName;
            if (channel.getName().matches("^\\[\\d*\\].*$"))
                newChannelName = channel.getName().replaceFirst("\\[\\d*\\]", String.format("[%03d]", privateChannel.getNumber()));
            else newChannelName = String.format("[%03d] %s", privateChannel.getNumber(), channel.getName());
            query.channelRename(newChannelName, privateChannel.getId());
        }
    }

    @Teamspeak3Command("!chso")
    @ClientGroupAccess("servergroups.admins")
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
            if (channel.getOwner() != PrivateChannel.FREE_CHANNEL_OWNER)
                query.setChannelGroup(channel.getOwner(), channel.getId(), config.getInt("channelgroups.guest[@id]"));
            channel.setOwner(clientDatabaseId);

            return new CommandResponse(config.getString("messages.chso[@success]")
                    .replace("$NUMBER$", Integer.toString(channel.getNumber()))
                    .replace("$CLDBID$", Integer.toString(channel.getOwner())));
        }
    }

    @Task(delay = 7 * 24 * 60 * 60 * 1000, day = 7, hour = 0, minute = 0, second = 0)
    public void moveChannelsUp() {
        synchronized (channelsLock) {
            for (int i = 0; i < channels.size(); i++) {
                PrivateChannel channel = channels.get(i);
                if (channel.isFree()) {
                    PrivateChannel usedChannel = findNextUsedChannel(i + 1);
                    if (usedChannel == null) break;

                    try {
                        query.channelMove(usedChannel.getId(), channel.getId());
                        query.channelDelete(channel.getId());

                        Channel channelInfo = query.getChannelInfo(usedChannel.getId());
                        String newName = String.format("[03%d]%s", channel.getNumber(), channelInfo.getName().substring(5));
                        query.channelRename(newName, usedChannel.getId());

                        messenger.addMessage(usedChannel.getOwner(), config.getString("messages.moved[@msg]").replace("$NUMBER$", Integer.toString(channel.getNumber())));

                        channel.setId(usedChannel.getId());
                        channel.setOwner(usedChannel.getOwner());

                        channels.remove(usedChannel);

                        for (int j = i + 1; j < channels.size(); j++)
                            channels.get(j).setNumber(channels.get(j).getNumber() - 1);

                    } catch (QueryException e) {
                        log.error("Failed to perform free channel cleanup!", e);
                    }
                }
            }
        }
    }

    private PrivateChannel findNextUsedChannel(int index) {
        for (int i = index; i < channels.size(); i++) {
            PrivateChannel channel = channels.get(i);
            if (!channel.isFree())
                return channel;
        }

        return null;
    }
}
