package com.staniul.configuration.annotations;

import com.staniul.configuration.ConfigurationLoader;
import java.lang.annotation.*;

/**
 * Used by {@link ConfigurationLoader} to load configuration for a class.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigFile {
    /**
     * Name of file in resources/config folder.
     * @return Name of file.
     */
    public String value() default "";
}
