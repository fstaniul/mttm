package com.staniul.core.events;

/**
 * Event that can occur on teamspeak 3 server. Supported types are: {@code Event<Client>} that is called when client
 * joins teamspeak 3 server and {@code Event<Integer>} that is called when client leaves teamspeak 3 server.
 *
 * @param <T> type of parameter that is passes to call method of event.
 */
public interface Event<T> {
    /**
     * Calls event.
     * @param t
     */
    void call(T t);
}
