package com.staniul.teamspeak.commands;

import com.staniul.teamspeak.query.Client;

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
