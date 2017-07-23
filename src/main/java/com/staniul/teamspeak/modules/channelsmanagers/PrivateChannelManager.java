package com.staniul.teamspeak.modules.channelsmanagers;

import com.staniul.teamspeak.modules.channelsmanagers.dao.PrivateChannel;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.QueryException;

public interface PrivateChannelManager {
    PrivateChannel createChannel (Client client) throws QueryException;
    boolean deleteChannel (int channelId);
    boolean changeChannelOwner (int channelId, int newOwnerId) throws QueryException;
    PrivateChannel getFreeChannel ();
    PrivateChannel getClientsChannel (Client client);
    PrivateChannel getClientsChannel (int clientDatabaseId);
}
