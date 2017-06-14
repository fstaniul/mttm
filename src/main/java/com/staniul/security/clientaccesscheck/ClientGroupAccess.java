package com.staniul.security.clientaccesscheck;

import java.lang.annotation.*;

/**
 * Methods and events annotated with this annotation are being checked before being invoked.
 * For command if access wont be granted appropriate message is sent.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ClientGroupAccessContainer.class)
public @interface ClientGroupAccess {
    /**
     * String that points to application.properties property with groups permitted given as list if integers divided by
     * comma.
     */
    String value();

    /**
     * Access check that will check if client has access to this command.
     * If left empty groups will be treated as servergroups.
     */
    Class<? extends ClientGroupAccessCheck> check() default ClientServergroupAccessCheck.class;
}
