package com.staniul.xmlconfig;

import java.lang.annotation.*;

/**
 * Fields annotated with this annotation needs to be in class annotated with CustomConfigLoad to make change.<br>
 * You can specify name of parameter that is going to be read from configuration.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigEntry {
    String value();
}
