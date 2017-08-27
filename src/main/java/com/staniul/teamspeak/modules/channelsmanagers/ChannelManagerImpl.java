package com.staniul.teamspeak.modules.channelsmanagers;

import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.ClientDatabase;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@UseConfig("modules/channelmanager.xml")
public class ChannelManagerImpl implements ChannelManager {
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
    public PrivateChannel createPrivateChannel(int databaseId) throws ChannelManagerException {
        PrivateChannel clientsChannel = getClientsPrivateChannel(databaseId);
        if (clientsChannel != null) return clientsChannel;

        PrivateChannel freeChannel = getFreePrivateChannel();
        if (freeChannel != null) { //There is free channel:
            clientsChannel = transformFreePrivateChannel(freeChannel, databaseId);
        }
        else {
            clientsChannel = createNewPrivateChannel(databaseId);
        }

        setChannelOwner(clientsChannel, databaseId);

        return clientsChannel;
    }

    @Override
    public PrivateChannel createPrivateChannel(Client client) throws ChannelManagerException {
        return createPrivateChannel(client.getDatabaseId());
    }

    private PrivateChannel transformFreePrivateChannel(PrivateChannel freeChannel, int databaseId) {
        try {
            ClientDatabase clientDatabase = query.getClientDatabaseInfo(databaseId);
            String channelName = config.getString("privatechannels.channelname")
                    .replace("%CHANNEL_NUMBER%", Integer.toString(freeChannel.getNumber()))
                    .replace("%CLIENT_NICKNAME%", clientDatabase.getNickname());
            int channelOrder = freeChannel.getId();

        } catch (QueryException ex) {

        }
    }

    @Override
    public PrivateChannel getClientsPrivateChannel(int databaseId) {
        return null;
    }

    @Override
    public PrivateChannel getClientsPrivateChannel(Client client) {
        return null;
    }

    @Override
    public PrivateChannel getFreePrivateChannel() {
        return null;
    }

    @Override
    public PrivateChannel getFreePrivateChannel(int number) {
        return null;
    }

    @Override
    public VipChannel createVipChannel(int databaseId) throws ChannelManagerException {
        return null;
    }

    @Override
    public VipChannel createVipChannel(Client client) throws ChannelManagerException {
        return createVipChannel(client.getDatabaseId());
    }

    @Override
    public VipChannel getClientsVipChannel(int databaseId) {
        return null;
    }

    @Override
    public VipChannel getClientsVipChannel(Client client) {
        return null;
    }

    private VipChannel queryForClientsVipChannel (int clientDatabaseId) {
        List<VipChannel> vipChannels = jdbcTemplate.query("SELECT * FROM vip_channels WHERE owner_id = ? LIMIT 1",
                new Object[] {clientDatabaseId},
                VipChannel.rowMapper());

        return vipChannels.size() == 1 ? vipChannels.get(0) : null;
    }

    private PrivateChannel queryForClientsPrivateChannel (int clientDatabaseId) {
        List<PrivateChannel> privateChannels = jdbcTemplate.query("SELECT * FROM private_channels WHERE owner_id = ? LIMIT 1",
                new Object[]{clientDatabaseId},
                PrivateChannel.rowMapper());

        return privateChannels.size() == 1 ? privateChannels.get(0) : null;
    }

    private List<VipChannel> queryForVipChannels () {
        return jdbcTemplate.query("SELECT * FROM vip_channels ORDER BY number ASC", VipChannel.rowMapper());
    }

    private List<PrivateChannel> queryFroPrivateChannels () {
        return jdbcTemplate.query("SELECT * FROM private_channels ORDER BY number ASC", PrivateChannel.rowMapper());
    }
}
