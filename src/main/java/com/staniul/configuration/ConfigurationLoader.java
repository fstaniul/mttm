package com.staniul.configuration;

import com.staniul.configuration.annotations.ConfigFile;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class ConfigurationLoader {
    private static final Configurations configurations = new Configurations();

    /**
     * Loads configuration for a class. Class needs to be annotated with {@link ConfigFile} annotation pointing to a
     * file in resources/config directory.
     *
     * @param aClass Class for which we load config.
     *
     * @return {@code XMLConfiguration} object that contains configuration for given class.
     *
     * @throws ConfigurationException If {@code Configurations} fails to read the configuration (file does not exists,
     *                                extension is not xml or any other IO exception occurs)
     */
    public static XMLConfiguration load(Class<?> aClass) throws ConfigurationException {
        if (!aClass.isAnnotationPresent(ConfigFile.class))
            throw new IllegalArgumentException(String.format("A class %s is not annotated with %s", aClass, ConfigFile.class));

        String configFile = aClass.getAnnotation(ConfigFile.class).value();

        return configurations.xml(ConfigurationLoader.class.getClassLoader().getResource("config/" + configFile));
    }
}
