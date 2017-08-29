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
     * Changes private channel owner.
     * @param channelNumber Channel number
     * @param ownerId Client database id
     * @return {@code true} on success, {@code false} if channel was not found.
     * @throws ChannelManagerException When there is an error with setting channel groups.
     */
    boolean changePrivateChannelOwner(int channelNumber, int ownerId) throws ChannelManagerException;

    /**
     * Changes channel number to free channel with given number
     * @param ownerId Database id of owner.
     * @param channelNumber Number of free channel to use.
     * @return {@code true} on success, {@code false} otherwise
     * @throws ChannelManagerException When there is an error with reordering channels.
     */
    boolean changePrivateChannelNumber(int ownerId, int channelNumber) throws  ChannelManagerException;

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
     * Changes owner of a VIP channel with given number.
     * @param channelNumber Number of vip channel.
     * @param ownerId Database id of a new owner.
     * @return {@code true} - success, {@code false} - not found
     * @throws ChannelManagerException when there is an error while changing owner of a vip channel.
     */
    boolean changeVipChannelOwner (int channelNumber, int ownerId) throws ChannelManagerException;

    /**
     * Gets clients vip channel or returns null if client does not have a vip channel.
     * @param databaseId Client database id.
     * @return {@code VipChannel} object containing information about client vip channel or {@code null} if client does
     * not have vip channel.
     */
    VipChannel getClientsVipChannel(int databaseId);

    VipChannel getClientsVipChannel(Client client);
}
