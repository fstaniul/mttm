package com.staniul.teamspeak.events;

import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccessCheck;
import com.staniul.teamspeak.security.clientaccesscheck.ClientServergroupAccessCheck;

import java.lang.annotation.*;

/**
 * <p>Methods annotated with this annotation are treated as teamspeak 3 events and will be invoked when teamspeak 3 server
 * notifies about event occurrence.<br /></p>
 * <p>Method signature should look like:<br />
 * - For Join event: {@code void * (Client *);}
 * - For Leave event: {@code void * (Integer *);}
 * </p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Teamspeak3Event {
    /**
     * Type of event, when it should occur.
     */
    EventType value();
}
