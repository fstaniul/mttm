package com.staniul.xmlconfig.di;

import org.springframework.beans.BeansException;

public class BeansXMLConfigurationException extends BeansException {
    public BeansXMLConfigurationException() {
        super("Failed to load beans xml configuration!");
    }

    public BeansXMLConfigurationException(Throwable cause) {
        super("Failed to load beans xml configuration!", cause);
    }
}
