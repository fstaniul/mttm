package com.staniul.teamspeak.query;

import com.staniul.teamspeak.TeamspeakCoreController;
import com.staniul.teamspeak.query.channel.ChannelProperties;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.util.lang.StringUtil;
import com.staniul.xmlconfig.annotations.WireConfig;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import de.stefan1200.jts3serverquery.TS3ServerQueryException;
import org.apache.commons.configuration2.Configuration;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@UseConfig("query.xml")
public class Query {
    /**
     * Apache Log4j logger to log errors and activities.
     */
    private static Logger log = Logger.getLogger(Query.class);

    /**
     * Backed jts3serverquery object for communication with teamspeak 3 server query.
     */
    private JTS3ServerQuery jts3ServerQuery;

    /**
     * Configuration of this class. Contains information about teamspeak 3 connection.
     */
    @WireConfig
    private Configuration configuration;

    /**
     * Thread that is keeping the connection up with teamspeak 3 server. Prevents server from timeouting application
     * after not sending message after few minutes, by sending every 2 minutes a dummy message.
     */
    private Thread connectionKeeper;

    /**
     * Indicates if method connect was already called or no, and if query should be connected with teamspeak 3 server
     * or not. Used in connection keeper to determine if it should try reconnecting.
     */
    private boolean connected;

    /**
     * Used to create a new query object. Should be only created by spring, since we only use one connection with
     * teamspeak 3 server.
     */
    @Autowired
    public Query() {
        this.jts3ServerQuery = new JTS3ServerQuery("Query");
        connected = false;
    }

    @Autowired
    public void setTeamspeakActionListener(TeamspeakCoreController coreController) {
        this.jts3ServerQuery.setTeamspeakActionListener(new TeamspeakActionListenerImpl(this, coreController));
    }

    /**
     * Connects to teamspeak 3 server.
     *
     * @throws Exception If jts3serverquery fails to establish connection with teamspeak 3 server.
     */
    @PostConstruct
    public void connect() throws Exception {
        internalConnect();
        connectionKeeper = new Thread(new ConnectionKeeper(), "Query Connection Keeper");
        connectionKeeper.start();
        this.connected = true;
    }

    /**
     * Connects to teamspeak 3 server with jts3serverquery.
     *
     * @throws Exception If jts3serverquery fails to establish connection with teamspeak 3 server.
     */
    private void internalConnect() throws Exception {
        jts3ServerQuery.connectTS3Query(configuration.getString("ip"), configuration.getInt("port"));
        jts3ServerQuery.loginTS3(configuration.getString("login"), configuration.getString("password"));
        jts3ServerQuery.selectVirtualServer(configuration.getInt("serverid"));
    }

    /**
     * Disconnects from teamspeak 3 server.
     * Kills connection keeper.
     */
    @PreDestroy
    public void disconnect() {
        connected = false;
        connectionKeeper.interrupt();
        jts3ServerQuery.closeTS3Connection();
    }

    /**
     * Gets client information from teamspeak 3 server about currently connected client with given id.
     *
     * @return {@code Client} object containing information about client with given id.
     *
     * @throws QueryException If server query request returns with error, client does not exists or probably when
     *                        query's been disconnected from teamspeak 3 server.
     */
    public Client getClientInfo(int clientId) throws QueryException {
        try {
            HashMap<String, String> clientInfo = jts3ServerQuery.getInfo(JTS3ServerQuery.INFOMODE_CLIENTINFO, clientId);
            return new Client(clientId, clientInfo);
        } catch (TS3ServerQueryException e) {
            throwQueryException("Failed to get client information from teamspeak 3 server.", e);
            return null;
        }
    }

    /**
     * Gets list of clients currently connected to teamspeak 3 server.
     * Clients are stored in List.
     *
     * @return {@code List<Client>} containing all clients currently connected to teamspeak 3 server.
     *
     * @throws QueryException If server query request returns with error, probably when query've been disconnected from
     *                        teamspeak 3 server.
     */
    public List<Client> getClientList() throws QueryException {
        try {
            Vector<HashMap<String, String>> clientList = jts3ServerQuery.getList(JTS3ServerQuery.LISTMODE_CLIENTLIST, "-uid,-away,-voice,-times,-groups,-info,-icon,-country,-ip");
            List<Client> result = new ArrayList<>(clientList.size());
            clientList.stream()
                    .map(c -> new Client(Integer.parseInt(c.get("clid")), c))
                    .forEach(result::add);
            return result;
        } catch (TS3ServerQueryException ex) {
            throwQueryException("Failed to get client list from teamspeak 3 server", ex);
            return null;
        }
    }

    /**
     * Gets client information from teamspeak 3 database. These are all offline information stored about client.
     * Too see what is stored see {@link ClientDatabase}.
     * If you want list of currently connected clients see {@link #getClientInfo(int)}.
     *
     * @param clientDatabaseId Database id of client.
     *
     * @return {@link ClientDatabase} object containing all database information about client.
     *
     * @throws QueryException If query fails to get information from teamspeak 3 server, client with given id does not
     *                        exists or there was a problem with connection with teamspeak 3 server.
     */
    public ClientDatabase getClientDatabaseInfo(int clientDatabaseId) throws QueryException {
        try {
            HashMap<String, String> clientInfo = jts3ServerQuery.getInfo(JTS3ServerQuery.INFOMODE_CLIENTDBINFO, clientDatabaseId);
            return new ClientDatabase(clientDatabaseId, clientInfo);
        } catch (TS3ServerQueryException e) {
            throwQueryException("Failed to get client database information from teamspeak 3 server.", e);
            return null;
        }
    }

    /**
     * Gets list of client information from teamspeak 3 database. These are all offline information stored about client.
     * Client database list is narrowed to one group.
     *
     * @param servergroupId Servergroup id.
     *
     * @return List of clients database info in this group.
     *
     * @throws QueryException When query fails to get information from teamspeak 3 server.
     */
    public List<ClientDatabase> getClientDatabaseListInServergroup(int servergroupId) throws QueryException {
        List<Integer> clientDatabaseIds = servergroupClientList(servergroupId);
        List<ClientDatabase> clientDatabaseList = new ArrayList<>(clientDatabaseIds.size());
        for (Integer clientDatabaseId : clientDatabaseIds)
            clientDatabaseList.add(getClientDatabaseInfo(clientDatabaseId));
        return clientDatabaseList;
    }

    /**
     * Gets channel information from teamspeak 3 server with given id.
     *
     * @param channelId Channel id.
     *
     * @return {@code Channel} object containing information about channel.
     *
     * @throws QueryException When query fails to get information from teamspeak 3 server, that could be channel does
     *                        not exists with given id or connection with teamspeak 3 server was interrupted.
     */
    public Channel getChannelInfo(int channelId) throws QueryException {
        try {
            HashMap<String, String> channelInfo = jts3ServerQuery.getInfo(JTS3ServerQuery.INFOMODE_CHANNELINFO, channelId);
            return new Channel(channelId, channelInfo);
        } catch (TS3ServerQueryException e) {
            throwQueryException("Failed to get channel information from teamspeak 3 server.", e);
            return null;
        }
    }

    /**
     * Gets channel list from teamspeak 3 server.
     *
     * @return {@code List<Channel>} containing information about channels currently present on teamspeak 3 server.
     *
     * @throws QueryException When query fails to get channel list from teamspeak 3 server, because connection with
     *                        teamspeak 3 being interrupted.
     */
    public List<Channel> getChannelList() throws QueryException {
        try {
            Vector<HashMap<String, String>> channelList = jts3ServerQuery.getList(JTS3ServerQuery.LISTMODE_CHANNELLIST, "-topic,-flags,-voice,-limits,-icon,-secondsempty");
            List<Channel> result = new ArrayList<>(channelList.size());
            channelList.stream().map(c -> new Channel(Integer.parseInt(c.get("cid")), c)).forEach(result::add);
            return result;
        } catch (TS3ServerQueryException e) {
            throwQueryException("Failed to get channel list from teamspeak 3 server.", e);
            return null;
        }
    }

    /**
     * Sets clients channel group for specified channel.
     *
     * @param clientDatabaseId Clients database id.
     * @param channelId        Channel id.
     * @param groupId          Channel group id.
     *
     * @throws QueryException When query fails to assign group for teamspeak 3 client.
     */
    public void setChannelGroup(int clientDatabaseId, int channelId, int groupId) throws QueryException {
        String template = "setclientchannelgroup cldbid=%d cid=%d cgid=%d";
        Map<String, String> serverResponse = jts3ServerQuery.doCommand(String.format(template, clientDatabaseId, channelId, groupId));
        checkAndThrowQueryException("Failed to set channel group for client!", serverResponse);
    }

    /**
     * Kicks client from server with reason {@code RULES VIOLATION}.
     *
     * @param clientId Client id.
     *
     * @throws QueryException When query fails to kick client from server, might be he disconnected before or connection
     *                        with teamspeak 3 server was interrupted.
     */
    public void kickClient(int clientId) throws QueryException {
        kickClient(clientId, "RULES VIOLATION");
    }

    /**
     * Kicks client from server with specified reason.
     *
     * @param clientId Client id.
     * @param msg      Reason for kick, will be displayed to client in kick message dialog.
     *
     * @throws QueryException When query fails to kick client from server, might be he disconnected before or connection
     *                        with teamspeak 3 server was interrupted.
     */
    public void kickClient(int clientId, String msg) throws QueryException {
        try {
            jts3ServerQuery.kickClient(clientId, false, msg);
        } catch (TS3ServerQueryException e) {
            throwQueryException("Failed to kick client from server.", e);
        }
    }

    /**
     * Kicks client from channel with specified message.
     *
     * @param clientId Client id.
     * @param msg      Reason for kick, will be displayed to client in kick message dialog.
     *
     * @throws QueryException When query fails to kick client from server, might be he disconnected before or connection
     *                        with teamspeak 3 server was interrupted.
     */
    public void kickClientFromChannel(int clientId, String msg) throws QueryException {
        try {
            jts3ServerQuery.kickClient(clientId, true, msg);
        } catch (TS3ServerQueryException e) {
            throwQueryException("Failed to kick client from channel.", e);
        }
    }

    /**
     * Pokes client with given id with given message.
     *
     * @param clientId Id of client.
     * @param message  Message to sent in as poke.
     * @param split    If {@code true} message will be split on space (" ") to not exceed 100 characters and send as
     *                 multiple poke messages.
     *
     * @throws QueryException When query fails to poke client, message is too long or client disconnected.
     */
    public void pokeClient(int clientId, String message, boolean split) throws QueryException {
        try {
            if (split) {
                String[] splitMessage = StringUtil.splitOnSize(message, " ", 100);
                for (String msg : splitMessage)
                    jts3ServerQuery.pokeClient(clientId, msg);
            }
            else {
                if (message.length() > 100)
                    message = message.substring(0, 100);
                jts3ServerQuery.pokeClient(clientId, message);
            }
        } catch (TS3ServerQueryException e) {
            throwQueryException(String.format("Failed to poke client (%d) with message (%s)", clientId, message), e);
        }
    }

    /**
     * Adds client to servergroup.
     *
     * @param clientDatabaseId Client database id.
     * @param servergroupId    Servergroup id.
     *
     * @throws QueryException When query fails to add client to a servergroup.
     */
    public void servergroupAddClient(int clientDatabaseId, int servergroupId) throws QueryException {
        String request = String.format("servergroupaddclient cldbid=%d sgid=%d", clientDatabaseId, servergroupId);
        Map<String, String> response = jts3ServerQuery.doCommand(request);
        checkAndThrowQueryException(String.format("Failed to add client (%d) to servergroup (%d)", clientDatabaseId, servergroupId), response);
    }

    /**
     * Removes client from servergroup.
     *
     * @param clientDatabaseId Client database id.
     * @param servergroupId    Servergroup id.
     *
     * @throws QueryException When query fails to remove client from servergroup.
     */
    public void servergroupDeleteClient(int clientDatabaseId, int servergroupId) throws QueryException {
        String request = String.format("servergroupdelclient cldbid=%d sgid=%d", clientDatabaseId, servergroupId);
        Map<String, String> response = jts3ServerQuery.doCommand(request);
        checkAndThrowQueryException(String.format("Failed to remove client (%d) from servergroup (%d)", clientDatabaseId, servergroupId), response);
    }

    /**
     * Gets list of clients database id's in servergroup of given id.
     *
     * @param servergroupId Id of a servergroup.
     *
     * @return List of clients database id's in this group.
     *
     * @throws QueryException When query fails to get client list from teamspeak 3 server.
     */
    public List<Integer> servergroupClientList(int servergroupId) throws QueryException {
        String request = String.format("servergroupclientlist sgid=%d", servergroupId);
        Map<String, String> response = jts3ServerQuery.doCommand(request);
        checkAndThrowQueryException(String.format("Failed to get list of clients in servergroup (%d)", servergroupId), response);
        return JTS3ServerQuery.parseRawData(response.get("response"))
                .stream()
                .map(e -> Integer.parseInt(e.get("cldbid")))
                .collect(Collectors.toList());
    }

    /**
     * Gets list of clients database ids in servergroups of given id. It just invokes {@link
     * #servergroupClientList(int)} for each group and adds result to one list.
     *
     * @param servergroupIds Ids of servergroups.
     *
     * @return List of clients database ids in those servergroups.
     *
     * @throws QueryException When query fails to get list of clients in servergroup.
     */
    public List<Integer> servergroupClientList(int... servergroupIds) throws QueryException {
        List<Integer> groupsIds = new ArrayList<>(servergroupIds.length);
        for (int servergroupId : servergroupIds)
            groupsIds.add(servergroupId);
        return servergroupClientList(groupsIds);
    }

    /**
     * Gets list of clients database ids in servergroups of given id. It just invokes {@link
     * #servergroupClientList(int)} for each group and adds result to one list.
     *
     * @param servergroupIds Ids of servergroups.
     *
     * @return List of clients database ids in those servergroups.
     *
     * @throws QueryException When query fails to get list of clients in servergroup.
     */
    public List<Integer> servergroupClientList(Integer... servergroupIds) throws QueryException {
        return servergroupClientList(Arrays.asList(servergroupIds));
    }

    /**
     * Gets list of clients database ids in servergroups of given id. It just invokes {@link
     * #servergroupClientList(int)} for each group and adds result to one list.
     *
     * @param servergroupIds Ids of servergroups.
     *
     * @return List of clients database ids in those servergroups.
     *
     * @throws QueryException When query fails to get list of clients in servergroup.
     */
    public List<Integer> servergroupClientList(Collection<Integer> servergroupIds) throws QueryException {
        List<Integer> result = new ArrayList<>();
        for (Integer groupId : servergroupIds)
            result.addAll(servergroupClientList(groupId));
        return result;
    }

    /**
     * Moves client to channel with given id.
     *
     * @param clientId  Id of client.
     * @param channelId Id of channel.
     *
     * @throws QueryException When query fails to move client. Client might have disconnected from server before this
     *                        call.
     */
    public void moveClient(int clientId, int channelId) throws QueryException {
        try {
            jts3ServerQuery.moveClient(clientId, channelId, "");
        } catch (TS3ServerQueryException e) {
            throwQueryException(String.format("Failed to move client (%d) to channel (%d)", clientId, channelId), e);
        }
    }

    /**
     * <p>Sends message to client. If message is too long it will divide it on spaces. If you want the message to be
     * divided otherwise then use {@link #sendTextMessageToClient(int, String[])} or {@link
     * #sendTextMessageToClient(int, Collection)}<br /></p>
     * <p>
     * <p>Teamspeak 3 max message length is 1024 byte long in UTF-8 encoding. For sake of simplicity messages are
     * divided after 512 chars on space. Since most characters uses 1 byte in UTF-8, in most case use it should be
     * sufficient.<br /> Otherwise you need to divide message by yourself or exception will be thrown.</p>
     *
     * @param clientId Id of client to whom we send message.
     * @param message  Message to send.
     *
     * @throws QueryException When teamspeak 3 query fails to send message.
     */
    public void sendTextMessageToClient(int clientId, String message) throws QueryException {
        sendTextMessageToClient(clientId, StringUtil.splitOnSize(message, " ", 512));
    }

    /**
     * Sends messages to client. Messages cannot be longer then 1024 bytes long.
     *
     * @param clientId Id of client to whom we send message.
     * @param messages Collection of Strings. Each string will be sent as separate message. Each message cannot be
     *                 longer then 1024 byte.
     *
     * @throws QueryException When messages are too long or teamspeak 3 query fails to send messages.
     */
    public void sendTextMessageToClient(int clientId, Collection<String> messages) throws QueryException {
        sendTextMessageToClient(clientId, (String[]) messages.toArray());
    }

    /**
     * Sends messages to client. Messages cannot be longer then 1024 bytes long.
     *
     * @param clientId Id of client to whom we send message.
     * @param messages Array of Strings containing messages to send. Each message must be max of 1024 bytes long.
     *
     * @throws QueryException When messages are too long or teamspeak 3 query fails to send the message.
     */
    public void sendTextMessageToClient(int clientId, String[] messages) throws QueryException {
        for (int i = 0; i < messages.length; i++) {
            String msg = messages[i];
            if (msg.getBytes().length > 1024)
                throw new QueryException("Messages are too long! Message too long index: " + i);
        }

        for (String msg : messages) {
            try {
                jts3ServerQuery.sendTextMessage(clientId, JTS3ServerQuery.TEXTMESSAGE_TARGET_CLIENT, msg);
            } catch (TS3ServerQueryException e) {
                throwQueryException("Failed to send message to client!", e);
            }
        }
    }

    /**
     * <p>Sends message to channel. If message is too long it will divide it on spaces. If you want the message to be
     * divided otherwise then use {@link #sendTextMessageToChannel(int, String[])} or {@link
     * #sendTextMessageToChannel(int, Collection)}<br /></p>
     * <p>
     * <p>Teamspeak 3 max message length is 1024 byte long in UTF-8 encoding. For sake of simplicity messages are
     * divided after 512 chars on space. Since most characters uses 1 byte in UTF-8, in most case use it should be
     * sufficient.<br /> Otherwise you need to divide message by yourself or exception will be thrown.</p>
     *
     * @param channelId Id of channel.
     * @param message   Message to send.
     *
     * @throws QueryException WHen teamspeak 3 server query fails to send message.
     */
    public void sendTextMessageToChannel(int channelId, String message) throws QueryException {
        sendTextMessageToChannel(channelId, StringUtil.splitOnSize(message, " ", 512));
    }

    /**
     * Sends messages to channel. Max message length is 1024 byte in UTF-8 encoding.
     *
     * @param channelId Id of channel.
     * @param messages  Messages to send.
     *
     * @throws QueryException When messages are too long or teamspeak 3 query fails to send messages.
     */
    public void sendTextMessageToChannel(int channelId, Collection<String> messages) throws QueryException {
        sendTextMessageToChannel(channelId, messages.toArray(new String[messages.size()]));
    }

    /**
     * Sends messages to channel. Max message length is 1024 byte in UTF-8 encoding.
     *
     * @param channelId Id of channel.
     * @param messages  Messages to send.
     *
     * @throws QueryException When messages are too long or teamspeak 3 query fails to send messages.
     */
    public void sendTextMessageToChannel(int channelId, String[] messages) throws QueryException {
        for (int i = 0; i < messages.length; i++) {
            String msg = messages[i];
            if (msg.getBytes().length > 1024)
                throw new QueryException("Messages are too long! Message too long index: " + i);
        }

        for (String msg : messages) {
            try {
                jts3ServerQuery.sendTextMessage(channelId, JTS3ServerQuery.TEXTMESSAGE_TARGET_CHANNEL, msg);
            } catch (TS3ServerQueryException e) {
                throwQueryException("Failed to send message to client!", e);
            }
        }
    }

    /**
     * Gets list of clients with assigned channel groups for channel with a given id.
     *
     * @param channelId Id of a channel.
     *
     * @return List of ClientChannelInfo object that contain information about clients groups for specified channel.
     *
     * @throws QueryException When query fails to get information from teamspeak 3 server.
     */
    public List<ClientChannelInfo> getChannelgroupClientList(int channelId) throws QueryException {
        String request = String.format("channelgroupclientlist cid=%d", channelId);

        Map<String, String> response = jts3ServerQuery.doCommand(request);
        checkAndThrowQueryException("Failed to get channelgroup client list from teamspeak 3 server.", response);

        Vector<HashMap<String, String>> parsed = JTS3ServerQuery.parseRawData(response.get("response"));
        return parsed.stream().map(ClientChannelInfo::new).collect(Collectors.toList());
    }

    /**
     * Creates channel from properties.
     *
     * @param properties Properties for new created channel.
     *
     * @return Id of newly created channel.
     *
     * @throws QueryException When query fails to create new teamspeak 3 channel.
     */
    public int channelCreate(ChannelProperties properties) throws QueryException {
        String request = "channelcreate " + properties.toTeamspeak3QueryString();
        HashMap<String, String> response = jts3ServerQuery.doCommand(request);
        checkAndThrowQueryException("Failed to create teamspeak 3 channel!", response);
        String channelId = JTS3ServerQuery.parseLine(response.get("response")).get("cid");
        return Integer.parseInt(channelId);
    }

    /**
     * Deletes channel with a given id.
     *
     * @param id Id of channel to delete.
     *
     * @throws QueryException When query fails to delete channel.
     */
    public void channelDelete(int id) throws QueryException {
        try {
            jts3ServerQuery.deleteChannel(id, true);
        } catch (TS3ServerQueryException e) {
            throwQueryException(String.format("Failed to delete channel (%d).", id), e);
        }
    }

    /**
     * Moves channel with given id to position after channel with given id.
     *
     * @param channelId      Id of channel to move.
     * @param afterChannelId Id of channel to place channel after it.
     */
    public void channelMove(int channelId, int afterChannelId) throws QueryException {
        String request = String.format("channeledit cid=%d channel_order=%d", channelId, afterChannelId);
        Map<String, String> response = jts3ServerQuery.doCommand(request);
        checkAndThrowQueryException(String.format("Failed to move channel (%d) after channel (%d)", channelId, afterChannelId), response);
    }

    /**
     * Renames channel with given id.
     *
     * @param newName   New name for channel.
     * @param channelId Channel id.
     *
     * @throws QueryException When query fails to change channel name, or channel name cannot be changed (it is already
     *                        in use in the same scope, or channel with given id does not exists).
     */
    public void channelRename(String newName, int channelId) throws QueryException {
        String request = String.format("channeledit cid=%d channel_name=%s", channelId, JTS3ServerQuery.encodeTS3String(newName));
        Map<String, String> response = jts3ServerQuery.doCommand(request);
        checkAndThrowQueryException("Failed to rename channel!", response);
    }

    /**
     * Changes channel description.
     *
     * @param description Channel description.
     * @param channelId   If of teamspeak 3 channel.
     *
     * @throws QueryException When query fails to edit channel's description.
     */
    public void channelChangeDescription(String description, int channelId) throws QueryException {
        String request = String.format("channeledit cid=%d channel_description=%s", channelId, JTS3ServerQuery.encodeTS3String(description));
        Map<String, String> response = jts3ServerQuery.doCommand(request);
        checkAndThrowQueryException("Failed to change channel description!", response);
    }

    /**
     * Throws {@link QueryException} based on {@link TS3ServerQueryException}. Used multiple times so its a helpful
     * method. You can specify message that will be added to exception.
     *
     * @param msg Additional message added to exception.
     * @param ex  {@code TS3ServerQueryException} on which {@code QueryException} should be based.
     *
     * @throws QueryException Created {@code QueryException} based on given {@code TS3ServerQueryException}
     */
    private void throwQueryException(String msg, TS3ServerQueryException ex) throws QueryException {
        throw new QueryException(msg, ex, ex.getErrorID(), ex.getErrorMessage());
    }

    /**
     * For information see {@link #throwQueryException(String, TS3ServerQueryException)}
     *
     * @param ex
     *
     * @throws QueryException
     * @see #throwQueryException(String, TS3ServerQueryException)
     */
    private void throwQueryException(TS3ServerQueryException ex) throws QueryException {
        throw new QueryException(ex, ex.getErrorID(), ex.getErrorMessage());
    }

    /**
     * Checks if response returned with error and if so throws exception, otherwise returns silently.
     *
     * @param serverResponse Response from teamspeak 3 server after invoking command.
     *
     * @throws QueryException When teamspeak 3 server query returned with error it throws exception.
     */
    private void checkAndThrowQueryException(Map<String, String> serverResponse) throws QueryException {
        if ("ok".equals(serverResponse.get("msg")) && "0".equals(serverResponse.get("id")))
            return;

        throw new QueryException(Integer.parseInt(serverResponse.get("id")), serverResponse.get("msg"));
    }

    /**
     * Same as {@link #checkAndThrowQueryException(Map)}, but lets you specify message in exception.
     *
     * @param msg            Message to be added to exception.
     * @param serverResponse Response from teamspeak 3 server after invoking command.
     *
     * @throws QueryException When teamspeak 3 server query returned with error it throws exception.
     */
    private void checkAndThrowQueryException(String msg, Map<String, String> serverResponse) throws QueryException {
        if ("ok".equals(serverResponse.get("msg")) && "0".equals(serverResponse.get("id")))
            return;

        throw new QueryException(msg, Integer.parseInt(serverResponse.get("id")), serverResponse.get("msg"));
    }

    /**
     * Every 2 minutes sends dummy message to keep up the connection with teamspeak 3 server preventing teamspeak 3
     * server query from not sending notification messages in case of no action performed on teamspeak 3 server.
     * Started with every connection with teamspeak 3 server. If teamspeak 3 drops connection with query, tries to
     * reconnect once.
     */
    private class ConnectionKeeper implements Runnable {
        @Override
        public void run() {
            try {
                while (connected) {
                    if (!jts3ServerQuery.isConnected()) internalConnect();
                    jts3ServerQuery.doCommand("Keeping connection up!");
                    Thread.sleep(TimeUnit.MINUTES.toMillis(2));
                }
            } catch (InterruptedException e) {
                log.info("Connection Keeper in Query stopped.");
            } catch (Exception e) {
                log.error("Connection Keeper in Query stopped because of exception!", e);
            }
        }
    }
}
