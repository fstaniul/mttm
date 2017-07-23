package com.staniul.teamspeak.modules.channelsmanagers;

import com.staniul.teamspeak.modules.channelsmanagers.dao.PrivateChannel;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.teamspeak.query.channel.ChannelFlagConstants;
import com.staniul.teamspeak.query.channel.ChannelProperties;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@UseConfig("modules/pcm.xml")
public class PrivateChannelManagerImpl implements PrivateChannelManager {
    private final JdbcTemplate database;
    private final Query query;

    @WireConfig
    private CustomXMLConfiguration config;

    public PrivateChannelManagerImpl(JdbcTemplate database, Query query) {
        this.database = database;
        this.query = query;
    }

    @Override
    public PrivateChannel createChannel(Client client) throws QueryException {
        PrivateChannel ret = getClientsChannel(client);

        if (ret != null) return ret;

        ret = getFreeChannel();

        if (ret == null) {
            int channelNumber = database.queryForObject("SELECT count(*) FROM private_channels", Integer.class) + 1;

            ChannelProperties properties = defaultClientsChannelProperties(client, channelNumber);

            int newChannelId = query.channelCreate(properties);

            ret = new PrivateChannel(channelNumber, newChannelId, client.getDatabaseId());

            database.update("INSERT INTO private_channels (number, id, owner) VALUES (?, ?, ?)",
                    ret.getNumber(), ret.getId(), ret.getOwner());
        }
        else {
            ChannelProperties properties = defaultClientsChannelProperties(client, ret.getNumber())
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
    public boolean deleteChannel(int channelId) {

    }

    @Override
    public boolean changeChannelOwner(int channelNumber, int newOwnerId) throws QueryException {
        String sql = "SELECT * FROM private_channels WHERE number = ?";
        PrivateChannel privateChannel = queryForPrivateChannel(sql, channelNumber);

        if (privateChannel == null) return false;

        query.setChannelGroup(newOwnerId, privateChannel.getId(), config.getInt("groups.channel.owner"));

        database.update("UPDATE private_channels SET owner = ? WHERE number = ?", newOwnerId, channelNumber);

        return true;
    }

    @Override
    public PrivateChannel getFreeChannel() {
        return queryForPrivateChannel("SELECT * FROM private_channels WHERE owner = '-1'");
    }

    @Override
    public PrivateChannel getClientsChannel(Client client) {
        return queryForPrivateChannel("SELECT * FROM private_channels WHERE owner = ?", client.getDatabaseId());
    }

    @Override
    public PrivateChannel getClientsChannel(int clientDatabaseId) {
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

    private ChannelProperties defaultClientsChannelProperties(Client client, int channelNumber) {
        String channelNumberString = String.format(config.getString("create-channel.number-format"), channelNumber);
        return new ChannelProperties()
                .setName(
                        config.getString("create-channel.client.name")
                                .replace("$NICKNAME$", client.getNickname())
                                .replace("$NUMBER$", channelNumberString)
                )
                .setDescription(
                        config.getString("create-channel.client.desc")
                                .replace("$NICKNAME$", client.getNickname())
                                .replace("$NUMBER$", channelNumberString)
                )
                .setParent(config.getInt("create-channel.parent"))
                .setCodec(4)
                .setCodecQuality(10)
                .setFlag(ChannelFlagConstants.MAXCLIENTS_UNLIMITED | ChannelFlagConstants.MAXFAMILYCLIENTS_UNLIMITED);
    }
}
