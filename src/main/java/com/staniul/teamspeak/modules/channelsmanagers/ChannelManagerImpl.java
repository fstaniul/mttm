package com.staniul.teamspeak.modules.channelsmanagers;

import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
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
        return null;
    }

    @Override
    public PrivateChannel createPrivateChannel(Client client) throws ChannelManagerException {
        return null;
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
        return null;
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
        String sql = "";

        List<VipChannel> vipChannel = jdbcTemplate.query("SELECT * FROM vip_channels WHERE owner_id = ? LIMIT 1",
                new Object[] {clientDatabaseId},
                (rs, rowNum) -> new VipChannel(rs.getInt("number"), )))
    }

}
