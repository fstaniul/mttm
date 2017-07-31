package com.staniul.teamspeak.modules.channelsmanagers.vipchannels;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.staniul.teamspeak.query.Channel;
import com.staniul.teamspeak.query.ClientDatabase;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.teamspeak.query.channel.ChannelFlagConstants;
import com.staniul.teamspeak.query.channel.ChannelProperties;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;

import java.util.List;

@Component
@UseConfig("modules/vipchannelmanager.xml")
public class VipChannelManagerImpl implements VipChannelManager {
    private static Logger log = LogManager.getLogger(VipChannelManagerImpl.class);

    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;
    private final JdbcTemplate database;

    public VipChannelManagerImpl(final Query query, final JdbcTemplate database) {
        this.query = query;
        this.database = database;
    }

    @Override
    public synchronized VipChannel createChannel(int clientDatabaseId) {
        VipChannel clientsChannel = getClientsChannel(clientDatabaseId);
        if (clientsChannel != null)
            return clientsChannel;

        List<VipChannel> vipChannels = queryForChannels();
        VipChannel last = vipChannels.get(vipChannels.size());

        int number = vipChannels.size() + 1;

        try {
            //Create separator
            int separatorId = query.channelCreate(getSeparatorProperties(number, last.getId()));

            //Get client info and create a channel for him:
            ClientDatabase clientDatabase = query.getClientDatabaseInfo(clientDatabaseId);
            int vipChannelId = query.channelCreate(getVipChannelProperties(separatorId, clientDatabase.getNickname()));

            //Update in database:
            String sql = "INSERT INTO vip_channels (number, id, owner) VALUES (?, ?, ?)";
            database.update(sql, number, vipChannelId, clientDatabaseId);

            return new VipChannel(number, vipChannelId, clientDatabaseId);
        } catch (QueryException e) {
            log.error("Failed to create a vip channel for client!", e);
            return null;
        }
    }

    @Override
    public boolean deleteChannel(int number) {
        VipChannel clientsChannel = queryForChannelByNumber(number);
        return deleteChannel(clientsChannel);
    }

    @Override
    public boolean deleteClientsChannel(int ownerDatabaseId) {
        VipChannel clientsChannel = queryForChannelByOwner(ownerDatabaseId);
        return deleteChannel(clientsChannel);
    }

    private boolean deleteChannel(VipChannel channel) {
        //Delete separator if it is not the last channel on the list:
        int channelsCount = queryForChannelsCount();
        if (channelsCount < channel.getNumber()) {
            try {
                Channel separatorAfter = query.getChannelList().stream().filter(c -> c.getOrderId() == channel.getId())
                        .findAny().orElse(null);
                if (separatorAfter != null) {
                    query.channelDelete(separatorAfter.getId());
                }
            } catch (QueryException e) {
                log.error("Failed to delete separator after vip channel!", e);
            }
        }

        try {
            query.channelDelete(channel.getId());
            return true;
        } catch (QueryException e) {
            log.error("Failed to delete vip channel!", e);
            return false;
        }
    }

    @Override
    public synchronized VipChannel getClientsChannel(int clientDatabaseId) {
        return queryForChannelByOwner(clientDatabaseId);
    }

    private VipChannel queryForChannelByOwner(int clientDatabaseId) {
        String sql = "SELECT * FROM vip_channels WHERE owner = ?";
        try {
            return database.queryForObject(sql, VipChannel.rowMapper(), clientDatabaseId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private VipChannel queryForChannelByNumber(int channelId) {
        String sql = "SELECT * FROM vip_channels WHERE number = ?";
        try {
            return database.queryForObject(sql, VipChannel.rowMapper(), channelId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private int queryForChannelsCount() {
        String sql = "SELECT count(*) FROM vip_channels";
        return database.queryForObject(sql, Integer.class);
    }

    private List<VipChannel> queryForChannels() {
        String sql = "SELECT * FROM vip_channels ORDER BY number ASC";
        return database.query(sql, VipChannel.rowMapper());
    }

    private ChannelProperties getSeparatorProperties(int number, int after) {
        return new ChannelProperties()
                .setName(config.getString("separator.name").replace("$NUMBER$", Integer.toString(number)))
                .setOrder(after).setMaxClients(0).setMaxFamilyClients(0);
    }

    private ChannelProperties getVipChannelProperties(int after, String clientNickname) {
        return new ChannelProperties()
                .setName(config.getString("vip-channel.name").replace("$NICKNAME", clientNickname)).setOrder(after)
                .setFlag(ChannelFlagConstants.MAXCLIENTS_UNLIMITED)
                .setFlag(ChannelFlagConstants.MAXFAMILYCLIENTS_UNLIMITED);
    }
}