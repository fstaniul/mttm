package com.staniul.core.security.clientaccesscheck;

import com.staniul.query.Client;

public class PermitAllClientAccessCheck implements ClientAccessCheck {
    @Override
    public Boolean apply(Client client) {
        return true;
    }
}
