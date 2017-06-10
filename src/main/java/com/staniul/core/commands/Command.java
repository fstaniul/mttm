package com.staniul.core.commands;

import com.staniul.query.Client;

public interface Command {
    CommandResponse invoke (Client client, String parameters) throws Exception;
}
