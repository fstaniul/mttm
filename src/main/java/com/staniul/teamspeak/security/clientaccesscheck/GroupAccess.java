package com.staniul.teamspeak.security.clientaccesscheck;

import java.lang.annotation.*;

/**
 * Methods and events annotated with this annotation are being checked before being invoked.
 * For command if access wont be granted appropriate message is sent.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(GroupAccessContainer.class)
public @interface GroupAccess {
    /**
     * Groups that are allowed to call this command.
     * If left empty, all clients are permitted to call this command.
     */
    int[] value();

    /**
     * Access check that will check if client has access to this command.
     * If left empty groups will be treated as servergroups.
     */
    Class<? extends ClientGroupAccessCheck> check() default ClientServergroupAccessCheck.class;
}
