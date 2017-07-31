package com.staniul.teamspeak.modules.channelsmanagers.vipchannels;

import com.staniul.teamspeak.modules.channelsmanagers.vipchannels.VipChannel;

public interface VipChannelManager {
    VipChannel createChannel (int ownerDatabaseId);
    VipChannel getClientsChannel (int ownerDatabaseId);
    boolean deleteChannel (int number);
    boolean deleteClientsChannel (int ownerDatabaseId);
    
}