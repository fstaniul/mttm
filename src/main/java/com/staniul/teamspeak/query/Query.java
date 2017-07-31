package com.staniul.teamspeak.query;

import de.stefan1200.jts3serverquery.TeamspeakActionListener;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.staniul.teamspeak.query.channel.ChannelProperties;
import com.staniul.teamspeak.query.servergroups.Servergroup;
import com.staniul.teamspeak.query.servergroups.ServergroupExtended;

public interface Query {
    void connect() throws Exception;

    void disconnect() throws Exception;

    void setTeamspeakActionListener(TeamspeakActionListener actionListener) throws Exception;

    Client getClientInfo(int clientId) throws QueryException;
    List<Client> getClientList () throws QueryException;
    ClientDatabase getClientDatabaseInfo (int clientDatabaseId) throws QueryException;
    List<ClientDatabase> getClientDatabaseListInServergroup (int servergroupId) throws QueryException;

    Channel getChannelInfo (int channelId) throws QueryException;
    List<Channel> getChannelList () throws QueryException;

    void setChannelGroup (int clientDatabaseId, int channelId, int groupId) throws QueryException;

    void pokeClient (int clientId, String message, boolean split) throws QueryException;
    void pokeClient (int clientId, Collection<String> message) throws QueryException;
    void pokeClient (int clientId, String[] messages) throws QueryException;

    void servergroupAddClient (int clientDatabaseId, int servergroupId) throws QueryException;
    void servergroupAddClient (int clientDatabaseId, int... servergroups) throws QueryException;
    void servergroupAddClient (int clientDatabaseId, Integer... servergroups) throws QueryException;
    void servergroupDeleteClient (int clientdatabaseId, int servergroupId) throws QueryException;
    void servergroupDeleteClient (int clientdatabaseId, int... servergroupId) throws QueryException;
    void servergroupDeleteClient (int clientdatabaseId, Integer... servergroupId) throws QueryException;

    List<Integer> servergroupClientList (int servergroupId) throws QueryException;
    List<Integer> servergroupClientList (int... servergroupId) throws QueryException;
    List<Integer> servergroupClientList (Integer... servergroupId) throws QueryException;
    List<Integer> servergroupClientList (Collection<Integer> servergroupId) throws QueryException;

    void moveClient (int clietnId, int channelId) throws QueryException;
    void kickClient (int clientId) throws QueryException;
    void kickClient (int clientId, String message) throws QueryException;
    void kickClientFromChannel (int clientId, String message) throws QueryException;

    void sendTextMessageToClient (int clientId, String message) throws QueryException;
    void sendTextMessageToClient (int clientId, String message, String delimiter) throws QueryException;
    void sendTextMessageToClient (int clientId, Collection<String> messages) throws QueryException;
    void sendTextMessageToClient (int clientId, String[] messages) throws QueryException;
    void sendTextMessageToChannel (int channelId, String message) throws QueryException;
    void sendTextMessageToChannel (int channelId, Collection<String> messages) throws QueryException;
    void sendTextMessageToChannel (int channelId, String[] messages) throws QueryException;

    List<ClientChannelInfo> getChannelgroupClientList (int channelId) throws QueryException;

    int channelCreate (ChannelProperties properties) throws QueryException;
    void channelDelete (int channelId) throws QueryException;
    void channelMove (int channelId, int afterChannelId) throws QueryException;
    void channelRename (String name, int channelId) throws QueryException;
    void channelChangeDescription (String description, int channelId) throws QueryException;

    List<ServergroupExtended> getServergroupsList () throws QueryException;
    List<ServergroupExtended> getServergroupsListNarrowedBySortId (int min, int max) throws QueryException;
    List<ServergroupExtended> getServergroupsListNarrowedBySortId (List<ServergroupExtended> servergroups, int min, int max);
    Map<Integer, ServergroupExtended> getServergroupsMap () throws QueryException;
    Map<Integer, ServergroupExtended> getServergroupsMap (List<ServergroupExtended> servergroup);
    Map<Integer, ServergroupExtended> getServergroupsMapNarrowedBySortId (int min, int max) throws QueryException;
    Map<Integer, ServergroupExtended> getServergroupsMapNarrowedBySortId (List<ServergroupExtended> servergroups, int min, int max);

    List<Servergroup> getServergroupsOfClient (int clientDatabaseId) throws QueryException;

}