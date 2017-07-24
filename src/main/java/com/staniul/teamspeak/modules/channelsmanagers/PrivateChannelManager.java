package com.staniul.teamspeak.modules.channelsmanagers;

import com.staniul.teamspeak.modules.channelsmanagers.dao.PrivateChannel;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.QueryException;

public interface PrivateChannelManager {
    PrivateChannel createChannel (Client client) throws QueryException;
    boolean deleteChannel (int channelNumber) throws QueryException;
    boolean changeChannelOwner (int channelNumber, int ownerDatabaseId) throws QueryException;
    PrivateChannel getFreeChannel ();
    PrivateChannel getClientsChannel (Client client);
    PrivateChannel getClientsChannel (int clientDatabaseId);
}
