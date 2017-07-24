package com.staniul.teamspeak.modules.channelsmanagers;

import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.commands.validators.IntegerParamsValidator;
import com.staniul.teamspeak.commands.validators.ValidateParams;
import com.staniul.teamspeak.modules.channelsmanagers.dao.PrivateChannel;
import com.staniul.teamspeak.query.Channel;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.teamspeak.query.channel.ChannelFlagConstants;
import com.staniul.teamspeak.query.channel.ChannelProperties;
import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.teamspeak.taskcontroller.Task;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@UseConfig("modules/pcm.xml")
public class PrivateChannelManagerImpl implements PrivateChannelManager {
    private static Logger log = LogManager.getLogger(PrivateChannelManagerImpl.class);

    private final JdbcTemplate database;
    private final Query query;

    @WireConfig
    private CustomXMLConfiguration config;

    @Autowired
    public PrivateChannelManagerImpl(JdbcTemplate database, Query query) {
        this.database = database;
        this.query = query;
    }

    @Override
    public synchronized PrivateChannel createChannel(Client client) throws QueryException {
        PrivateChannel ret = getClientsChannel(client);

        if (ret != null) return ret;

        ret = getFreeChannel();

        if (ret == null) {
            int channelNumber = database.queryForObject("SELECT count(*) FROM private_channels", Integer.class) + 1;

            ChannelProperties properties = getClientsChannelProperties(client, channelNumber);

            int newChannelId = query.channelCreate(properties);

            ret = new PrivateChannel(channelNumber, newChannelId, client.getDatabaseId());

            database.update("INSERT INTO private_channels (number, id, owner) VALUES (?, ?, ?)",
                    ret.getNumber(), ret.getId(), ret.getOwner());
        }
        else {
            ChannelProperties properties = getClientsChannelProperties(client, ret.getNumber())
                    .setOrder(ret.getId());

            int newChannelId = query.channelCreate(properties);
            query.channelDelete(ret.getId());

            ret = new PrivateChannel(ret.getNumber(), newChannelId, client.getDatabaseId());

            database.update("UPDATE private_channels SET owner = ?, id = ? WHERE number = ?",
                    ret.getOwner(), ret.getId(), ret.getNumber());
        }

        query.setChannelGroup(ret.getOwner(), ret.getId(), config.getInt("groups.channel.owner"));

        createSubChannels(ret.getId());

        return ret;
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

    @Override
    public synchronized boolean deleteChannel(int channelNumber) throws QueryException {
        int numberOfChannels = database.queryForObject("SELECT count(*) FROM private_channels", Integer.class);

        if (channelNumber < 1 || channelNumber > numberOfChannels) {
            return false;
        }

        PrivateChannel channelToDelete = queryForPrivateChannel("SELECT * FROM private_channels WHERE number = ?", channelNumber);
        if (channelToDelete == null || channelToDelete.isFree()) return false;

        if (channelNumber == numberOfChannels) {
            query.channelDelete(channelToDelete.getId());
            database.update("DELETE FROM private_channels WHERE number = ?", channelNumber);
        }
        else {
            PrivateChannel free = createFreeChannel(channelToDelete);
            query.channelDelete(channelToDelete.getId());
            database.update("UPDATE private_channels SET owner = '-1', id = ? WHERE number = ?", free.getId(), channelNumber);
        }

        return true;
    }

    private PrivateChannel createFreeChannel(PrivateChannel channelToReplace) throws QueryException {
        ChannelProperties properties = getFreeChannelProperties(channelToReplace.getId(), channelToReplace.getNumber());
        int channelId = query.channelCreate(properties);
        return new PrivateChannel(channelToReplace.getNumber(), channelId, -1);
    }

    @Override
    public synchronized boolean changeChannelOwner(int channelNumber, int ownerDatabaseId) throws QueryException {
        String sql = "SELECT * FROM private_channels WHERE number = ?";
        PrivateChannel privateChannel = queryForPrivateChannel(sql, channelNumber);

        if (privateChannel == null) return false;

        query.setChannelGroup(ownerDatabaseId, privateChannel.getId(), config.getInt("groups.channel.owner"));

        database.update("UPDATE private_channels SET owner = ? WHERE number = ?", ownerDatabaseId, channelNumber);

        return true;
    }

    @Override
    public synchronized PrivateChannel getFreeChannel() {
        return queryForPrivateChannel("SELECT * FROM private_channels WHERE owner = '-1'");
    }

    @Override
    public synchronized PrivateChannel getClientsChannel(Client client) {
        return queryForPrivateChannel("SELECT * FROM private_channels WHERE owner = ?", client.getDatabaseId());
    }

    @Override
    public synchronized PrivateChannel getClientsChannel(int clientDatabaseId) {
        return queryForPrivateChannel("SELECT * FROM private_channels WHERE owner = ?", clientDatabaseId);
    }

    private PrivateChannel queryForPrivateChannel(String sql, Object... args) {
        try {
            return database.queryForObject(sql, (rs, i) -> new PrivateChannel(
                    rs.getInt("number"),
                    rs.getInt("id"),
                    rs.getInt("owner")
            ), args);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private String formatChannelNumber(int number) {
        return String.format(config.getString("create-channel.number-format"), number);
    }

    private ChannelProperties getClientsChannelProperties(Client client, int channelNumber) {
        String channelNumberString = formatChannelNumber(channelNumber);
        return new ChannelProperties()
                .setName(
                        config.getString("create-channel.client.name")
                                .replace("$NICKNAME$", client.getNickname())
                                .replace("$NUMBER$", channelNumberString)
                )
                .setDescription(
                        config.getString("create-channel.client.desc")
                                .replaceAll("[ \\t]{2,}", " ")
                                .replace("$NICKNAME$", client.getNickname())
                                .replace("$NUMBER$", channelNumberString)
                )
                .setParent(config.getInt("create-channel.channel-ids.parent"))
                .setCodec(4)
                .setCodecQuality(10)
                .setFlag(ChannelFlagConstants.MAXCLIENTS_UNLIMITED | ChannelFlagConstants.MAXFAMILYCLIENTS_UNLIMITED);
    }

    private ChannelProperties getFreeChannelProperties(int afterChannelId, int channelNumber) {
        String channelNumberString = formatChannelNumber(channelNumber);
        return new ChannelProperties()
                .setName(
                        config.getString("create-channel.free.name")
                                .replace("$NUMBER$", channelNumberString)
                )
                .setDescription(
                        config.getString("create-channel.free.desc")
                                .replaceAll("[ \\t]{2,}", " ")
                                .replace("$NUMBER$", Integer.toString(channelNumber))
                )
                .setTopic(
                        config.getString("create-channel.free.topic")
                                .replace("$NUMBER$", Integer.toString(channelNumber))
                )
                .setMaxClients(0)
                .setMaxFamilyClients(0)
                .setCodec(4)
                .setCodecQuality(10)
                .setOrder(afterChannelId);
    }

    @Task(delay = 5 * 1000)
    public synchronized void createChannelsForWaitingClientsMainLooop() throws QueryException {
        int eventChannelId = config.getInt("create-channel.channel-ids.event");
        query.getClientList().stream()
                .filter(c -> c.getCurrentChannelId() == eventChannelId)
                .forEach(this::createChannelForWaitingClient);
    }

    private void createChannelForWaitingClient(Client client) {
        PrivateChannel clientsChannel;
        try {
            clientsChannel = createChannel(client);
        } catch (QueryException e) {
            log.error("Failed to create a channel for client!", e);
            try {
                query.sendTextMessageToClient(client.getId(), config.getString("messages.channel-create.failed"));
            } catch (QueryException e1) {
                log.error("Failed to send text message to client!", e1);
            }
            return;
        }

        try {
            query.sendTextMessageToClient(client.getId(),
                    config.getString("messages.channel-create.success")
                            .replace("$NUMBER$", Integer.toString(clientsChannel.getNumber()))
                            .replace("$NUMBER_STR$", formatChannelNumber(clientsChannel.getNumber()))
            );
        } catch (QueryException e) {
            log.error("Failed to send text message to client!", e);
        }

        try {
            query.moveClient(client.getId(), clientsChannel.getId());
        } catch (QueryException e) {
            log.error("Failed to move clients to his private channel!", e);
        }
    }

    @Teamspeak3Command("!chdel")
    @ClientGroupAccess("servergroups.admins")
    @ValidateParams(IntegerParamsValidator.class)
    public synchronized CommandResponse deletePrivateChannelCommand(Client client, String params) throws QueryException {
        int channelNumber = Integer.parseInt(params);
        if (deleteChannel(channelNumber)) {
            return new CommandResponse(
                    config.getString("messages.channel-delete.msg")
                            .replace("$NUMBER$", Integer.toString(channelNumber))
            );
        }
        else {
            return new CommandResponse(
                    config.getString("messages.channel-delete.error")
                            .replace("$NUMBER$", Integer.toString(channelNumber))
            );
        }
    }

    @Task(delay = 5 * 60 * 1000)
    public synchronized void checkChannelsMainLoop() {
        List<PrivateChannel> privateChannels = getPrivateChannelsFromDatabase();
        Map<Integer, Channel> channelInfoMap = getChannelInfoMapFromServer(privateChannels);
        fixChannelNames(privateChannels, channelInfoMap);
        checkIfShouldBeMoved(privateChannels, channelInfoMap);

        privateChannels = getPrivateChannelsFromDatabase();
        channelInfoMap = getChannelInfoMapFromServer(privateChannels);
        checkForNotUsedChannel(privateChannels, channelInfoMap);
    }

    private List<PrivateChannel> getPrivateChannelsFromDatabase() {
        return database.query("SELECT * FROM private_channels", ((resultSet, i) -> new PrivateChannel(
                resultSet.getInt("number"),
                resultSet.getInt("id"),
                resultSet.getInt("owner")
        )));
    }

    private void checkIfShouldBeMoved(List<PrivateChannel> privateChannels, Map<Integer, Channel> channelInfoMap) {
        Pattern moveCheck = Pattern.compile("MOVE (\\d+)");
        for (PrivateChannel privateChannel : privateChannels) {
            Channel channel = channelInfoMap.get(privateChannel.getId());
            Matcher matcher = moveCheck.matcher(channel.getName());

            if (matcher.find()) {
                int moveNumber = Integer.parseInt(matcher.group(1));
                PrivateChannel channelToSwapWith = privateChannels.stream()
                        .filter(c -> c.getNumber() == moveNumber)
                        .findFirst()
                        .orElse(null);

                if (channelToSwapWith != null && channelToSwapWith.isFree()) {
                    try {
                        //create free channel
                        PrivateChannel free = createFreeChannel(privateChannel);
                        query.channelMove(privateChannel.getId(), channelToSwapWith.getId());
                        query.channelDelete(channelToSwapWith.getId());
                        query.channelRename(
                                "[" + formatChannelNumber(channelToSwapWith.getNumber()) + "]" + channel.getName().substring(5),
                                privateChannel.getId()
                        );

                        database.update("UPDATE private_channels SET id = ?, owner = ? WHERE number = ?",
                                privateChannel.getId(),
                                privateChannel.getOwner(),
                                channelToSwapWith.getNumber()
                        );

                        database.update("UPDATE private_channels SET id = ?, owner = '-1' WHERE number = ?",
                                free.getId(),
                                privateChannel.getNumber()
                        );
                    } catch (QueryException e) {
                        log.error("Failed to swap channels in place! ", e);
                    }
                }
            }
        }
    }

    private Map<Integer, Channel> getChannelInfoMapFromServer(List<PrivateChannel> privateChannels) {
        HashMap<Integer, Channel> channelInfoMap = new HashMap<>();
        for (PrivateChannel privateChannel : privateChannels) {
            try {
                Channel channel = query.getChannelInfo(privateChannel.getId());
                channelInfoMap.putIfAbsent(privateChannel.getId(), channel);
            } catch (QueryException e) {
                log.error("Failed to get info from server about channel with id " + privateChannel.getId(), e);
            }
        }

        return channelInfoMap;
    }

    private void checkForNotUsedChannel(List<PrivateChannel> privateChannels, Map<Integer, Channel> infoMap) {
        long secondsEmptyBeforeDeletion = config.getLong("delete-channel.seconds-empty");

        for (int i = privateChannels.size() - 1; i >= 0; i--) {
            PrivateChannel privateChannel = privateChannels.get(i);
            Channel channel = infoMap.get(privateChannel.getId());
            if (channel == null) {
                log.error("Missing information about channel with id " + privateChannel.getId() + " and number " + privateChannel.getNumber());
                continue;
            }

            try {
                if (channel.getSecondsEmpty() >= secondsEmptyBeforeDeletion) {
                    deleteChannel(privateChannel.getNumber());
                }
            } catch (QueryException e) {
                log.error("Failed to delete channel with number " + privateChannel.getNumber(), e);
            }
        }
    }

    private void fixChannelNames(List<PrivateChannel> privateChannels, Map<Integer, Channel> channelInfoMap) {
        Pattern pattern = Pattern.compile("\\[(\\d{3})](.*)");
        for (PrivateChannel channel : privateChannels) {
            try {
                Channel channelInfo = channelInfoMap.get(channel.getId());
                if (channelInfo == null) {
                    log.error("Missing information about channel with id " + channel.getId() + " and number " + channel.getNumber());
                    continue;
                }

                Matcher nameMatcher = pattern.matcher(channelInfo.getName());

                if (nameMatcher.find()) {
                    String actualNumber = nameMatcher.group(1);
                    String expectedNumber = formatChannelNumber(channel.getNumber());

                    if (!actualNumber.equals(expectedNumber)) {
                        fixChannelName(channel, channelInfo);
                    }
                }
                else fixChannelName(channel, channelInfo);
            } catch (QueryException e) {
                log.error("Failed to fix channel name with id " + channel.getId(), e);
            }
        }
    }

    private void fixChannelName(PrivateChannel channel, Channel channelInfo) throws QueryException {
        String newChannelName = "[" + formatChannelNumber(channel.getNumber()) + "]" + channelInfo.getName().substring(5);
        query.channelRename(newChannelName, channel.getId());
    }

    @Task(delay = 7 * 24 * 60 * 60 * 1000, day = 1, hour = 0, minute = 5, second = 0)
    public synchronized void stripFromFreeChannels() {
        List<PrivateChannel> freeChannels = database.query("SELECT * FROM private_channels WHERE owner = '-1' ORDER BY number DESC",
                (resultSet, i) -> new PrivateChannel(
                        resultSet.getInt("number"),
                        resultSet.getInt("id"),
                        resultSet.getInt("owner")
                ));

        for (PrivateChannel freeChannel : freeChannels) {
            try {
                query.channelDelete(freeChannel.getId());
                database.update("DELETE FROM private_channels WHERE number = ?", freeChannel.getNumber());
                database.update("UPDATE private_channels SET number = number - 1 WHERE number > ?", freeChannel.getNumber());
            } catch (QueryException e) {
                log.error("Failed to delete free channel with number " + freeChannel.getNumber());
            }
        }
    }
}
