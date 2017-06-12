package com.staniul.teamspeak.security.clientaccesscheck;

import java.lang.annotation.*;

/**
 * Container annotation for {@code @GroupAccess} annotation to make it repeatable.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GroupAccessContainer {
    GroupAccess[] value();
}
