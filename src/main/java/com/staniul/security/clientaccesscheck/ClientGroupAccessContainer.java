package com.staniul.security.clientaccesscheck;

import java.lang.annotation.*;

/**
 * Container annotation for {@code @ClientGroupAccess} annotation to make it repeatable.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ClientGroupAccessContainer {
    ClientGroupAccess[] value();
}
