package com.staniul.teamspeak.modules.channelsmanagers.privatechannels;

import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.QueryException;

public interface PrivateChannelManager {
    PrivateChannel createChannel (int clientDatabaseId, String clientNickname) throws QueryException;
    PrivateChannel deleteChannel (int channelNumber) throws QueryException;
    boolean changeChannelOwner (int channelNumber, int ownerDatabaseId) throws QueryException;
    PrivateChannel getFreeChannel ();
    PrivateChannel getClientsChannel (Client client);
    PrivateChannel getClientsChannel (int clientDatabaseId);
}
