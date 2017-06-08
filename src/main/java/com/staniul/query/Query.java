package com.staniul.query;

import com.staniul.configuration.annotations.ConfigFile;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import org.apache.commons.configuration2.Configuration;
import org.apache.log4j.Logger;

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
     * @param configuration A {@link Configuration} containing connection information for teamspeak 3 server.
     */
    public Query (Configuration configuration) {
        this.configuration = configuration;
        this.jts3ServerQuery = new JTS3ServerQuery("Query");
        connected = false;
    }

    /**
     * Connects to teamspeak 3 server.
     * @throws Exception If jts3serverquery fails to establish connection with teamspeak 3 server.
     */
    public void connect () throws Exception {
        internalConnect();
        connectionKeeper = new Thread(new ConnectionKeeper(), "Query Connection Keeper");
        connectionKeeper.start();
        this.connected = true;
    }

    /**
     * Connects to teamspeak 3 server with jts3serverquery.
     * @throws Exception If jts3serverquery fails to establish connection with teamspeak 3 server.
     */
    private void internalConnect () throws Exception {
        jts3ServerQuery.connectTS3Query(configuration.getString("ip"), configuration.getInt("port"));
        jts3ServerQuery.loginTS3(configuration.getString("login"), configuration.getString("password"));
        jts3ServerQuery.selectVirtualServer(configuration.getInt("serverid"));
    }

    /**
     * Disconnects from teamspeak 3 server.
     * Kills connection keeper.
     */
    public void disconnect () {
        connected = false;
        jts3ServerQuery.closeTS3Connection();
        connectionKeeper.interrupt();
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
