package com.staniul.teamspeak.modules.channelsmanagers;

import com.staniul.teamspeak.query.Client;

public interface ChannelManager {
    /**
     * Creates private channel for client with specified id as an owner.
     * @param databaseId Database id of client.
     * @return {@code PrivateChannel} object that contains information about newly created private channel.
     *
     * @throws ChannelManagerException When there was an error with creating private channel for client.
     */
    PrivateChannel createPrivateChannel(int databaseId) throws ChannelManagerException;

    PrivateChannel createPrivateChannel(Client client) throws ChannelManagerException;

    /**
     * Returns clients private channel if it exists, ie client is an owner of one of private channels.
     * @param databaseId Database id of client.
     * @return {@code PrivateChannel} object containing information about clients channel or {@code null} if it was not
     * found.
     */
    PrivateChannel getClientsPrivateChannel(int databaseId);

    PrivateChannel getClientsPrivateChannel(Client client);

    /**
     * Returns first found free private channel.
     * @return {@code PrivateChannel object containing info about free channel or {@code null} if there was no free
     * channel found.
     */
    PrivateChannel getFreePrivateChannel();

    /**
     * Returns first free private channel after the number given as parameter.
     * @param number Number of private channel.
     * @return {@code PrivateChannel} object containing information about channel or {@code null} if channel was not
     * found.
     */
    PrivateChannel getFreePrivateChannel(int number);

    /**
     * Creates a vip channel for clients and gives him vip groups on teamspeak 3 server.
     * @param databaseId Client database id.
     * @return {@code VipChannel} object containing information about vip channel.
     *
     * @throws ChannelManagerException When there is an error with vip channel creation.
     */
    VipChannel createVipChannel(int databaseId) throws ChannelManagerException;

    VipChannel createVipChannel(Client client) throws ChannelManagerException;

    /**
     * Gets clients vip channel or returns null if client does not have a vip channel.
     * @param databaseId Client database id.
     * @return {@code VipChannel} object containing information about client vip channel or {@code null} if client does
     * not have vip channel.
     */
    VipChannel getClientsVipChannel(int databaseId);

    VipChannel getClientsVipChannel(Client client);
}
