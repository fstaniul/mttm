package com.staniul.teamspeak.commands;

import com.staniul.query.Client;
import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccessCheck;
import com.staniul.teamspeak.security.clientaccesscheck.ClientServergroupAccessCheck;

import java.lang.annotation.*;

/**
 * Methods annotated with this annotation should have following signature:
 * {@link CommandResponse} * ({@link Client} client, {@link String} params)
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Teamspeak3Command {
    /**
     * Name of command that can be called by clients.
     */
    String value();
}
