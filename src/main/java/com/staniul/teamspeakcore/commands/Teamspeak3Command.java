package com.staniul.teamspeakcore.commands;

import com.staniul.query.Client;
import com.staniul.teamspeakcore.security.clientaccesscheck.ClientGroupAccessCheck;
import com.staniul.teamspeakcore.security.clientaccesscheck.ClientServergroupAccessCheck;

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

    /**
     * Groups that are allowed to call this command.
     * If left empty, all clients are permitted to call this command.
     */
    int[] groups() default {};

    /**
     * Access check that will check if client has access to this command.
     * If left empty groups will be treated as servergroups.
     */
    Class<? extends ClientGroupAccessCheck> check() default ClientServergroupAccessCheck.class;
}
