package com.staniul.security.clientaccesscheck;

import com.staniul.teamspeak.query.Client;

/**
 * Subclass of ClientGroupAccessCheck that permits all clients.
 * Used as default if groups() of @ClientGroupAccess annotation are
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
