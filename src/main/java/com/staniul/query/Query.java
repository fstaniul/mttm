package com.staniul.query;

import com.staniul.configuration.annotations.ConfigFile;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import de.stefan1200.jts3serverquery.TS3ServerQueryException;
import org.apache.commons.configuration2.Configuration;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;

@ConfigFile("query.xml")
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
     *
     * @param configuration A {@link Configuration} containing connection information for teamspeak 3 server.
     */
    public Query(Configuration configuration) {
        this.configuration = configuration;
        this.jts3ServerQuery = new JTS3ServerQuery("Query");
        connected = false;
    }

    /**
     * Connects to teamspeak 3 server.
     *
     * @throws Exception If jts3serverquery fails to establish connection with teamspeak 3 server.
     */
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
    public void disconnect() {
        connected = false;
        jts3ServerQuery.closeTS3Connection();
        connectionKeeper.interrupt();
    }

    /**
     * Gets client information from teamspeak 3 server about currently connected client with given id.
     *
     * @return {@code Client} object containing information about client with given id.
     *
     * @throws QueryException If server query request returns with error, client does not exists or probably when
     *                        query've been disconnected from teamspeak 3 server.
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
     * Clients are stored in ArrayList.
     *
     * @return {@code ArrayList<Client>} containing all clients currently connected to teamspeak 3 server.
     *
     * @throws QueryException If server query request returns with error, probably when query've been disconnected from
     *                        teamspeak 3 server.
     */
    public ArrayList<Client> getClientList() throws QueryException {
        try {
            Vector<HashMap<String, String>> clientList = jts3ServerQuery.getList(JTS3ServerQuery.LISTMODE_CLIENTLIST, "-uid,-away,-voice,-times,-groups,-info,-icon,-country,-ip");
            ArrayList<Client> result = new ArrayList<>(clientList.size());
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
