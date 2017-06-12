package com.staniul.teamspeak.security.clientaccesscheck;

import com.staniul.query.Client;

import java.util.Set;

/**
 * Subclass of ClientGroupAccessCheck that permits all clients.
 * Used as default if groups() of @GroupAccess annotation are
 */
public class ClientPermitAllAccessCheck extends ClientGroupAccessCheck {
    public ClientPermitAllAccessCheck() {
        super(null);
    }

    @Override
    public Boolean apply(Client client) {
        return true;
    }
}
