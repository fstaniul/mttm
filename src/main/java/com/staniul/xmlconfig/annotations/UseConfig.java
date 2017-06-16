package com.staniul.xmlconfig.annotations;

import com.staniul.xmlconfig.ConfigurationLoader;

import java.lang.annotation.*;

/**
 * Used by {@link ConfigurationLoader} to load configuration for a class.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UseConfig {
    /**
     * Name of file in resources/config folder.
     * @return Name of file.
     */
    String value();
}
