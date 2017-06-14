package com.staniul.xmlconfig;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class ConfigurationLoader {
    private static final Configurations configurations = new Configurations();

    /**
     * Loads configuration for a class. Class needs to be annotated with {@link UseConfig} annotation pointing to a
     * file in resources/config directory.
     *
     * @param aClass Class for which we load config.
     *
     * @return {@code XMLConfiguration} object that contains configuration for given class.
     *
     * @throws ConfigurationException If {@code Configurations} fails to read the configuration (file does not exists,
     *                                extension is not xml or any other IO exception occurs)
     */
    public static CustomXMLConfiguration load(Class<?> aClass) throws ConfigurationException {
        if (!aClass.isAnnotationPresent(UseConfig.class))
            throw new IllegalArgumentException(String.format("A class %s is not annotated with %s", aClass, UseConfig.class));

        String configFile = aClass.getAnnotation(UseConfig.class).value();

        XMLConfiguration xmlConfig = configurations.xml(ConfigurationLoader.class.getClassLoader().getResource("config/" + configFile));

        return new CustomXMLConfiguration(xmlConfig);
    }
}
