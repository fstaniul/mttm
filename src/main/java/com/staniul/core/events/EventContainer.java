package com.staniul.core.events;

import com.staniul.core.security.AccessCheck;

/**
 * Container for event.
 * @param <T> Type of parameter passed to call method of event and parameter for checker type.
 */
public class EventContainer <T> {
    private Event<T> event;
    private AccessCheck<T> accessCheck;

    public EventContainer(Event<T> event, AccessCheck<T> accessCheck) {
        this.event = event;
        this.accessCheck = accessCheck;
    }

    /**
     * Checks if event should be called and calls it.
     * @param t parameter passed to call method of event.
     */
    public void call (T t) {
        if (accessCheck.apply(t)) {
            event.call(t);
        }
    }
}
