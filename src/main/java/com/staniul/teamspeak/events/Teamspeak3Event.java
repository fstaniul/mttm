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
    Types value();

    /**
     * Groups for which event should be fired. If no groups where specified then event will be called on every client.
     */
    int[] groups() default {};

    /**
     * Access check to be used when checking. Default one is {@link ClientServergroupAccessCheck}
     */
    Class<? extends ClientGroupAccessCheck> check() default ClientServergroupAccessCheck.class;

    enum Types {
        JOIN, LEAVE;
    }
}
