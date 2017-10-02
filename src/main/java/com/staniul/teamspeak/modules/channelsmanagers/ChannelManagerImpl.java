package com.staniul.teamspeak.modules.channelsmanagers;

import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.commands.validators.IntegerParamsValidator;
import com.staniul.teamspeak.commands.validators.TwoIntegerParamsValidator;
import com.staniul.teamspeak.commands.validators.ValidateParams;
import com.staniul.teamspeak.query.*;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@UseConfig("modules/channelmanager.xml")
public class ChannelManagerImpl implements ChannelManager {
    private static Logger log = LogManager.getLogger(ChannelManagerImpl.class);

    @WireConfig
    private CustomXMLConfiguration config;

    private final Query query;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ChannelManagerImpl(Query query, JdbcTemplate jdbcTemplate) {
        this.query = query;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public synchronized PrivateChannel createPrivateChannel(int databaseId) throws ChannelManagerException {
        PrivateChannel clientsChannel = getClientsPrivateChannel(databaseId);
        if (clientsChannel != null) return clientsChannel;

        PrivateChannel freeChannel = getFreePrivateChannel();
        if (freeChannel != null) {
            clientsChannel = transformFreePrivateChannel(freeChannel, databaseId);
        }
        else {
            clientsChannel = createNewPrivateChannel(databaseId);
        }

        return clientsChannel;
    }

    @Override
    public PrivateChannel createPrivateChannel(Client client) throws ChannelManagerException {
        return createPrivateChannel(client.getDatabaseId());
    }

    private synchronized PrivateChannel transformFreePrivateChannel(PrivateChannel freeChannel, int databaseId) throws ChannelManagerException {
        try {
            ClientDatabase clientDatabase = query.getClientDatabaseInfo(databaseId);

            String channelName = config.getString("privatechannel.name")
                    .replace("%CHANNEL_NUMBER%", String.format("%03d", freeChannel.getNumber()))
                    .replace("%CLIENT_NICKNAME%", clientDatabase.getNickname());
            String description = config.getString("privatechannel.description")
                    .replace("%CHANNEL_NUMBER%", String.format("%03d", freeChannel.getNumber()))
                    .replace("%CLIENT_NICKNAME%", clientDatabase.getNickname());
            int channelOrder = freeChannel.getId();
            int parent = config.getInt("privatechannel.parent");


            ChannelProperties properties = new ChannelProperties()
                    .setName(channelName)
                    .setOrder(channelOrder)
                    .setDescription(description)
                    .setParent(parent)
                    .setCodec(4)
                    .setCodecQuality(10)
                    .setFlag(ChannelProperties.MAXCLIENTS_UNLIMITED | ChannelProperties.MAXFAMILYCLIENTS_UNLIMITED);

            int channelId = query.channelCreate(properties);
            PrivateChannel clientsChannel = new PrivateChannel(channelId, freeChannel.getNumber(), databaseId);

            jdbcTemplate.update("UPDATE private_channels SET channel_id = ?, owner_id = ? WHERE number = ?", channelId, databaseId, freeChannel.getNumber());

            query.channelDelete(freeChannel.getId());
            query.setChannelGroup(databaseId, channelId, config.getInt("channelgroups.private.owner"));

            createSubChannels(channelId);

            return clientsChannel;
        } catch (QueryException ex) {
            throw new ChannelManagerException(ex.getErrorId(), ex.getMessage(), ex);
        }
    }

    private synchronized PrivateChannel createNewPrivateChannel(int databaseId) throws ChannelManagerException {
        try {
            List<PrivateChannel> numberOfChannels = jdbcTemplate.query("SELECT * FROM private_channels ORDER BY number DESC LIMIT 1", PrivateChannel.rowMapper());
            int channelNumber = numberOfChannels.size() == 0 ? 1 : numberOfChannels.get(0).getNumber() + 1;

            ClientDatabase clientDatabase = query.getClientDatabaseInfo(databaseId);

            String channelName = config.getString("privatechannel.name")
                    .replace("%CHANNEL_NUMBER%", String.format("%03d", channelNumber))
                    .replace("%CLIENT_NICKNAME%", clientDatabase.getNickname());
            String description = config.getString("privatechannel.description")
                    .replace("%CHANNEL_NUMBER%", String.format("%03d", channelNumber))
                    .replace("%CLIENT_NICKNAME%", clientDatabase.getNickname());
            int parent = config.getInt("privatechannel.parent");

            ChannelProperties properties = new ChannelProperties()
                    .setName(channelName)
                    .setDescription(description)
                    .setParent(parent)
                    .setCodec(4)
                    .setCodecQuality(10)
                    .setFlag(ChannelProperties.MAXCLIENTS_UNLIMITED | ChannelProperties.MAXFAMILYCLIENTS_UNLIMITED);

            int channelId = query.channelCreate(properties);
            PrivateChannel clientsChannel = new PrivateChannel(channelId, channelNumber, databaseId);

            jdbcTemplate.update("INSERT INTO private_channels (number, channel_id, owner_id) VALUES (?, ?, ?)", channelNumber, channelId, databaseId);

            query.setChannelGroup(databaseId, channelId, config.getInt("channelgroups.private.owner"));

            createSubChannels(channelId);

            return clientsChannel;
        } catch (QueryException ex) {
            throw new ChannelManagerException(ex.getErrorId(), ex.getMessage(), ex);
        }
    }

    private void createSubChannels(int channelId) throws QueryException {
        ChannelProperties properties = new ChannelProperties()
                .setParent(channelId)
                .setFlag(ChannelProperties.MAXCLIENTS_UNLIMITED | ChannelProperties.MAXFAMILYCLIENTS_UNLIMITED)
                .setCodec(4)
                .setCodecQuality(10);

        query.channelCreate(properties.setName("#1"));
        query.channelCreate(properties.setName("#2"));
    }

    private void createVipSubChannels(int channelId) throws QueryException {
        ChannelProperties properties = new ChannelProperties()
                .setParent(channelId)
                .setFlag(ChannelProperties.MAXCLIENTS_UNLIMITED | ChannelProperties.MAXFAMILYCLIENTS_UNLIMITED)
                .setCodec(4)
                .setCodecQuality(10);

        query.channelCreate(properties.setName("#1"));
        query.channelCreate(properties.setName("#2"));
        query.channelCreate(properties.setName("#3"));
        query.channelCreate(properties.setName("#4"));
        query.channelCreate(properties.setName("#5"));
    }

    @Override
    public synchronized boolean changePrivateChannelOwner(int channelNumber, int ownerId) throws ChannelManagerException {
        PrivateChannel privateChannel = queryForPrivateChannel(channelNumber);

        if (privateChannel == null) return false;

        try {
            changePrivateChannelOwnerInner(privateChannel.getId(), ownerId);
            jdbcTemplate.update("UPDATE private_channels SET owner_id = ? WHERE number = ?", ownerId, channelNumber);
        } catch (QueryException e) {
            throw new ChannelManagerException(e.getErrorId(), e.getMessage(), e);
        }

        return true;
    }

    @Override
    public synchronized boolean changePrivateChannelNumber(int ownerId, int channelNumber) throws ChannelManagerException {
        PrivateChannel clientsChannel = queryForClientsPrivateChannel(ownerId);
        PrivateChannel freeChannel = queryForPrivateChannel(channelNumber);

        if (clientsChannel == null || freeChannel == null || !freeChannel.isFree()) {
            return false;
        }

        try {
            int freeChannelId = createFreeChannel(clientsChannel.getNumber(), clientsChannel.getId());
            query.channelMove(clientsChannel.getId(), freeChannel.getId());
            query.channelDelete(freeChannel.getId());

            //UPDATE:
            jdbcTemplate.update("UPDATE private_channels SET channel_id = ?, owner_id = ? WHERE number = ?", clientsChannel.getId(), clientsChannel.getOwner(), freeChannel.getNumber());
            jdbcTemplate.update("UPDATE private_channels SET channel_id = ?, owner_id = '-1' WHERE number = ?", freeChannelId, clientsChannel.getNumber());

            return true;
        } catch (QueryException e) {
            throw new ChannelManagerException(e.getErrorId(), e.getMessage(), e);
        }
    }

    @Override
    public PrivateChannel getClientsPrivateChannel(int databaseId) {
        return queryForClientsPrivateChannel(databaseId);
    }

    @Override
    public PrivateChannel getClientsPrivateChannel(Client client) {
        return getClientsPrivateChannel(client.getDatabaseId());
    }

    @Override
    public PrivateChannel getFreePrivateChannel() {
        List<PrivateChannel> freeChannels = jdbcTemplate.query("SELECT * FROM private_channels WHERE owner_id = '-1' ORDER BY number ASC LIMIT 1", PrivateChannel.rowMapper());
        return freeChannels.size() == 1 ? freeChannels.get(0) : null;
    }

    @Override
    public PrivateChannel getFreePrivateChannel(int number) {
        List<PrivateChannel> freeChannels = jdbcTemplate.query("SELECT * FROM private_channels WHERE owner_id = '-1' AND number > ? ORDER BY number ASC LIMIT 1", new Object[]{number}, PrivateChannel.rowMapper());
        return freeChannels.size() == 1 ? freeChannels.get(0) : null;
    }

    @Override
    public synchronized VipChannel createVipChannel(int databaseId) throws ChannelManagerException {
        List<VipChannel> vipChannels = queryForVipChannels();

        try {
            ClientDatabase clientDatabase = query.getClientDatabaseInfo(databaseId);

            int order = vipChannels.size() == 0 ? config.getInt("vipchannel.after") : vipChannels.get(vipChannels.size() - 1).getSpacerId();
            String name = config.getString("vipchannel.name").replace("%CLIENT_NICKNAME%", clientDatabase.getNickname());

            ChannelProperties properties = new ChannelProperties()
                    .setName(name)
                    .setOrder(order)
                    .setCodec(4)
                    .setCodecQuality(10)
                    .setFlag(ChannelFlagConstants.MAXCLIENTS_UNLIMITED | ChannelFlagConstants.MAXFAMILYCLIENTS_UNLIMITED);

            int id = query.channelCreate(properties);

            int number = vipChannels.size();
            String spacerName = config.getString("vipchannel.spacer.name").replace("%NUMBER%", Integer.toString(number));

            ChannelProperties spacerProperties = new ChannelProperties()
                    .setName(spacerName)
                    .setOrder(id)
                    .setMaxClients(0)
                    .setMaxFamilyClients(0)
                    .setCodec(0)
                    .setCodecQuality(0);

            int spacerId = query.channelCreate(spacerProperties);

            VipChannel clientsChannel = new VipChannel(number, id, databaseId, spacerId);

            jdbcTemplate.update("INSERT INTO vip_channels (number, channel_id, owner_id, spacer_id) VALUES (?, ?, ?, ?)", number, id, databaseId, spacerId);

            query.setChannelGroup(databaseId, id, config.getInt("channelgroups.vip.owner"));

            Set<Integer> vipGroups = config.getIntSet("servergroups.vip");
            for (Integer vipGroup : vipGroups)
                query.servergroupAddClient(databaseId, vipGroup);

            createVipSubChannels(id);

            return clientsChannel;
        } catch (QueryException ex) {
            throw new ChannelManagerException(ex.getErrorId(), ex.getMessage(), ex);
        }
    }

    @Override
    public VipChannel createVipChannel(Client client) throws ChannelManagerException {
        return createVipChannel(client.getDatabaseId());
    }

    @Override
    public synchronized boolean changeVipChannelOwner(int channelNumber, int ownerId) throws ChannelManagerException {
        VipChannel vipChannel = queryForVipChannel(channelNumber);

        //Not found return false according to spec.
        if (vipChannel == null) return false;

        Set<Integer> vipGroups = config.getIntSet("servergroups.vip");
        int guestGroup = config.getInt("channelgroups.guest");
        int ownerGroup = config.getInt("channelgroups.vip.owner");

        //Remove previous channel owner from groups:
        vipGroups.forEach(g -> {
            try {
                query.servergroupDeleteClient(vipChannel.getOwnerId(), g);
            } catch (QueryException e) {
                log.error(e);
            }
        });
        try {
            query.setChannelGroup(vipChannel.getOwnerId(), vipChannel.getChannelId(), guestGroup);
        } catch (QueryException e) {
            throw new ChannelManagerException(e.getErrorId(), e.getMessage(), e);
        }

        //Add new channel owner and update database records:
        vipGroups.forEach(g -> {
            try {
                query.servergroupAddClient(ownerId, g);
            } catch (QueryException e) {
                log.error(e);
            }
        });
        try {
            query.setChannelGroup(ownerId, vipChannel.getChannelId(), ownerGroup);
        } catch (QueryException e) {
            throw new ChannelManagerException(e.getErrorId(), e.getMessage(), e);
        }

        jdbcTemplate.update("UPDATE vip_channels SET owner_id = ? WHERE number = ?", ownerId, channelNumber);

        return true;
    }

    @Override
    public VipChannel getClientsVipChannel(int databaseId) {
        return queryForClientsVipChannel(databaseId);
    }

    @Override
    public VipChannel getClientsVipChannel(Client client) {
        return getClientsVipChannel(client.getDatabaseId());
    }

    private VipChannel queryForClientsVipChannel(int clientDatabaseId) {
        List<VipChannel> vipChannels = jdbcTemplate.query("SELECT * FROM vip_channels WHERE owner_id = ? LIMIT 1",
                new Object[]{clientDatabaseId},
                VipChannel.rowMapper());

        return vipChannels.size() == 1 ? vipChannels.get(0) : null;
    }

    private PrivateChannel queryForClientsPrivateChannel(int clientDatabaseId) {
        List<PrivateChannel> privateChannels = jdbcTemplate.query("SELECT * FROM private_channels WHERE owner_id = ? LIMIT 1",
                new Object[]{clientDatabaseId},
                PrivateChannel.rowMapper());

        return privateChannels.size() == 1 ? privateChannels.get(0) : null;
    }

    private PrivateChannel queryForPrivateChannel(int number) {
        List<PrivateChannel> privateChannels = jdbcTemplate.query("SELECT * FROM private_channels WHERE number = ?", new Object[]{number}, PrivateChannel.rowMapper());
        return privateChannels.size() == 1 ? privateChannels.get(0) : null;
    }

    private List<VipChannel> queryForVipChannels() {
        return jdbcTemplate.query("SELECT * FROM vip_channels ORDER BY number ASC", VipChannel.rowMapper());
    }

    private List<PrivateChannel> queryForPrivateChannels() {
        return jdbcTemplate.query("SELECT * FROM private_channels ORDER BY number ASC", PrivateChannel.rowMapper());
    }

    private VipChannel queryForVipChannel(int number) {
        List<VipChannel> vipChannels = jdbcTemplate.query("SELECT * FROM vip_channels WHERE number = ?", new Object[]{number}, VipChannel.rowMapper());
        return vipChannels.size() == 1 ? vipChannels.get(0) : null;
    }

    private synchronized void createChannelForClient(Client client) {
        try {
            try {
                PrivateChannel channel = createPrivateChannel(client);

                String message = config.getString("messages.private.user.create.success")
                        .replace("%CHANNEL_NUMBER%", Integer.toString(channel.getNumber()));

                query.sendTextMessageToClient(client.getId(), message);
                query.moveClient(client.getId(), channel.getId());
            } catch (ChannelManagerException ex) {
                query.sendTextMessageToClient(client.getId(), config.getString("messages.private.user.create.error"));
            }
        } catch (QueryException ignore) {}
    }

    private synchronized int deleteChannel(int channelNumber) throws QueryException {
        List<PrivateChannel> channels = queryForPrivateChannels();
        PrivateChannel channel = channels.stream()
                .filter(ch -> ch.getNumber() == channelNumber)
                .findFirst()
                .orElse(null);

        if (channel == null) return 1;
        if (channel.isFree()) return 2;

        if (channels.indexOf(channel) == channels.size() - 1) { //LAST ONE
            query.channelDelete(channel.getId());
            jdbcTemplate.update("DELETE FROM private_channels WHERE number = ?", channel.getNumber());
            return 0;
        }

        else { //NOT LAST SO REPLACE WITH FREE
            int newChannelId = createFreeChannel(channel.getNumber(), channel.getId());
            query.channelDelete(channel.getId());
            jdbcTemplate.update("UPDATE private_channels SET channel_id = ?, owner_id = ? WHERE number = ?", newChannelId, -1, channel.getNumber());
            return 0;
        }
    }

    private int createFreeChannel(int number, int order) throws QueryException {
        String name = config.getString("freechannel.name")
                .replace("%CHANNEL_NUMBER%", String.format("%03d", number));
        String topic = config.getString("freechannel.topic")
                .replace("%CHANNEL_NUMBER%", Integer.toString(number));
        String description = config.getString("freechannel.description")
                .replace("%CHANNEL_NUMBER%", Integer.toString(number));
        int parent = config.getInt("privatechannel.parent");

        ChannelProperties properties = new ChannelProperties()
                .setName(name)
                .setTopic(topic)
                .setDescription(description)
                .setParent(parent)
                .setOrder(order)
                .setMaxClients(0)
                .setMaxFamilyClients(0)
                .setCodec(0)
                .setCodecQuality(0);

        return query.channelCreate(properties);
    }

    private void changePrivateChannelOwnerInner(int channelId, int owner) throws QueryException {
        List<ClientChannelInfo> clientChannelInfos = query.getChannelgroupClientList(channelId)
                .stream()
                .filter(cci -> cci.getChannelgroupId() == config.getInt("channelgroups.private.owner"))
                .collect(Collectors.toList());

        int guestGroup = config.getInt("channelgroups.guest");
        int ownerGroup = config.getInt("channelgroups.private.owner");

        try {
            for (ClientChannelInfo clientChannelInfo : clientChannelInfos) {
                query.setChannelGroup(clientChannelInfo.getClientDatabaseId(), channelId, guestGroup);
            }
        } catch (QueryException ignore) {}

        query.setChannelGroup(owner, channelId, ownerGroup);
    }


    @Task(delay = 5000)
    public void checkForClients() throws QueryException {
        int eventChannelId = config.getInt("eventchannelid");
        List<Client> clientsWaiting = query.getClientList().stream()
                .filter(client -> client.getCurrentChannelId() == eventChannelId)
                .collect(Collectors.toList());

        if (clientsWaiting.size() > 0) {
            for (Client client : clientsWaiting)
                createChannelForClient(client);
        }
    }

    @Task(delay = 24 * 60 * 60 * 1000, day = 1, hour = 0, minute = 5, second = 0)
    public synchronized void checkPrivateChannels() throws QueryException {
        List<PrivateChannel> privateChannels = queryForPrivateChannels();
        List<Channel> teamspeak3Channels = query.getChannelList();
        Pattern channelNamePattern = Pattern.compile("(\\[\\d{3}\\]).*");
        int secondsEmptyBeforeDelte = config.getInt("privatechannel.empty");

        for (PrivateChannel privateChannel : privateChannels) {
            Channel channelInfo = teamspeak3Channels.stream()
                    .filter(ch -> ch.getId() == privateChannel.getId())
                    .findFirst()
                    .orElse(null);

            if (channelInfo == null) {
                log.error("THERE IS ERROR IN PRIVATE CHANNELS DATABASE!!!");
                log.error("THERE IS ERROR IN PRIVATE CHANNELS DATABASE!!!");
                log.error("THERE IS ERROR IN PRIVATE CHANNELS DATABASE!!!");
                log.error("THERE IS ERROR IN PRIVATE CHANNELS DATABASE!!!");
                continue;
            }

            if (!channelNamePattern.matcher(channelInfo.getName()).matches()) {
                String channelName = String.format("[%03d]%s", privateChannel.getNumber(),
                        channelInfo.getName().length() < 5 ? "" : channelInfo.getName().substring(5));

                try {
                    query.channelRename(channelName, privateChannel.getId());
                } catch (QueryException ex) {
                    log.error("Failed to rename channel" + ex.getLocalizedMessage(), ex);
                }
            }

            if (!privateChannel.isFree()) {
                if (channelInfo.getSecondsEmpty() > secondsEmptyBeforeDelte) {
                    try {
                        deleteChannel(privateChannel.getNumber());
                    } catch (QueryException e) {
                        log.error("Failed to delete private channel after the time expired!", e);
                    }
                }
            }
        }
    }

    @Teamspeak3Command("!chdel")
    @ClientGroupAccess("servergroups.admins")
    @ValidateParams(IntegerParamsValidator.class)
    public CommandResponse deleteChannelCommand(Client client, String params) throws QueryException {
        int channelNumber = Integer.parseInt(params);
        try {
            int errCode = deleteChannel(channelNumber);
            if (errCode == 0) {
                String message = config.getString("messages.private.admin.delete.success")
                        .replace("%CHANNEL_NUMBER%", params);
                return new CommandResponse(message);
            }
            else if (errCode == 1) {
                String message = config.getString("messages.private.admin.delete.notfound").replace("%CHANNEL_NUMBER%", params);
                return new CommandResponse(message);
            }
            else if (errCode == 2) {
                String message = config.getString("messages.private.admin.delete.free")
                        .replace("%CHANNEL_NUMBER%", params);
                return new CommandResponse(message);
            }
        } catch (QueryException e) {
            query.sendTextMessageToClient(client.getId(), config.getString("messages.delete.admin.error"));
        }

        return new CommandResponse("UNKNOWN ERROR CODE! Contact administrators!");
    }

    @Teamspeak3Command("!chso")
    @ClientGroupAccess("servergroups.admins")
    @ValidateParams(TwoIntegerParamsValidator.class)
    public CommandResponse changeChannelOwnerCommand(Client client, String params) {
        Matcher matcher = TwoIntegerParamsValidator.getPattern().matcher(params);

        if (matcher.find()) {
            int channelNumber = Integer.parseInt(matcher.group(1));
            int owner = Integer.parseInt(matcher.group(2));


            try {
                String response;

                if (changePrivateChannelOwner(channelNumber, owner))
                    response = config.getString("message.private.admin.changeowner.success");

                else response = config.getString("messages.private.admin.changeowner.notfound");

                response = response
                        .replace("%CHANNEL_NUMBER%", String.format("%03d", channelNumber))
                        .replace("%CLIENT_ID%", Integer.toString(owner));

                return new CommandResponse(response);
            } catch (ChannelManagerException e) {
                String message = config.getString("message.private.admin.changeowner.error");
                return new CommandResponse(message);
            }
        }

        return new CommandResponse("PARAMS DID NOT MATCH THE REQUIRED FORM");
    }

    @Teamspeak3Command("!ccn")
    @ValidateParams(IntegerParamsValidator.class)
    public CommandResponse changeChannelNumberCommand(Client client, String params) {
        int channelNumber = Integer.parseInt(params);

        String response;

        try {
            if (changePrivateChannelNumber(client.getDatabaseId(), channelNumber)) {
                response = config.getString("messages.private.user.changenumber.success");
            }
            else response = config.getString("messages.private.user.changenumber.notfound");

        } catch (ChannelManagerException e) {
            log.error(e);
            response = config.getString("messages.private.user.changenumber.error");
        }

        response = response
                .replace("%CHANNEL_NUMBER%", params);

        return new CommandResponse(response);
    }

    @Teamspeak3Command("!cvc")
    @ClientGroupAccess("servergroups.headadmins")
    @ValidateParams(IntegerParamsValidator.class)
    public CommandResponse createVipChannelForClientCommand(Client client, String params) {
        int clientDatabaseId = Integer.parseInt(params);
        try {
            createVipChannel(clientDatabaseId);
            return new CommandResponse(config.getString("messages.vip.admin.create.success"));
        } catch (ChannelManagerException e) {
            log.error("Failed to create a vip channel for client " + e.getLocalizedMessage(), e);
            return new CommandResponse(config.getString("messages.vip.admin.create.error") + e.getMessage());
        }
    }

    @Teamspeak3Command("!vcl")
    @ClientGroupAccess("servergroups.headadmins")
    public CommandResponse vipChanneListCommand(Client client, String params) throws QueryException {
        List<VipChannel> vipChannels = queryForVipChannels();
        List<Channel> channels = query.getChannelList();

        String labelMsg = "nr: nazwa | id | wlasciciel";
        query.sendTextMessageToClient(client.getId(), labelMsg);

        for (VipChannel vipChannel : vipChannels) {
            Channel channelInfo = channels.stream()
                    .filter(ch -> ch.getId() == vipChannel.getChannelId())
                    .findFirst()
                    .orElse(null);

            if (channelInfo == null) {
                return new CommandResponse(config.getString("messages.vip.admin.list.dataerror") + " " + vipChannel);
            }

            String message = String.format("%d: %s | %d | %d", vipChannel.getNumber(), channelInfo.getName(), vipChannel.getChannelId(), vipChannel.getOwnerId());
            query.sendTextMessageToClient(client.getId(), message);
        }

        return new CommandResponse();
    }


    @Teamspeak3Command("!cvco")
    @ClientGroupAccess("servergroups.headadmins")
    @ValidateParams(TwoIntegerParamsValidator.class)
    public CommandResponse changeVipChannelOwnerCommand(Client client, String params) throws ChannelManagerException {
        Pattern pattern = TwoIntegerParamsValidator.getPattern();
        Matcher matcher = pattern.matcher(params);

        if (matcher.find()) {
            int channelNumber = Integer.parseInt(matcher.group(1));
            int newOwnerId = Integer.parseInt(matcher.group(2));

            String response;
            if (changeVipChannelOwner(channelNumber, newOwnerId)) {
                response = config.getString("messages.vip.admin.changeowner.success");
            }
            else {
                response = config.getString("messages.vip.admin.changeowner.notfound");
            }

            response = response.replace("%CHANNEL_NUMBER%", Integer.toString(channelNumber));
            return new CommandResponse(response);
        }

        return new CommandResponse("INVALID PARAMETERS!");
    }

    public boolean deleteVipChannel (int channelNumber) throws QueryException {
        VipChannel vipChannel = queryForVipChannel(channelNumber);

        if (vipChannel == null) return false;

        query.channelDelete(vipChannel.getChannelId());
        query.channelDelete(vipChannel.getSpacerId());

        jdbcTemplate.update("DELETE from vip_channels WHERE number = ?", channelNumber);
        jdbcTemplate.update("UPDATE vip_channels SET number = number - 1 WHERE number > ?", channelNumber);

        Set<Integer> vipGroups = config.getIntSet("servergroups.vip");
        for (Integer vipGroup : vipGroups) {
            try {
                query.servergroupDeleteClient(vipChannel.getOwnerId(), vipGroup);
            } catch (QueryException e) {
                log.error("Failed to remove client from vip servergroup", e);
            }
        }

        return true;
    }

    @Teamspeak3Command("!vcdel")
    @ClientGroupAccess("servergroups.headadmins")
    @ValidateParams(IntegerParamsValidator.class)
    public CommandResponse deleteVipChannelCommand (Client client, String params) throws ChannelManagerException {
        int channelNumber = Integer.parseInt(params);
        try {
            if (deleteVipChannel(channelNumber)) {
                return new CommandResponse(config.getString("messages.vip.admin.delete.success"));

            }else {
                return new CommandResponse(config.getString("messages.vip.admin.delete.failed"));
            }
        } catch (QueryException e) {
            throw new ChannelManagerException(112,"Failed to delete vip channel with number " + params, e);
        }
    }
}
