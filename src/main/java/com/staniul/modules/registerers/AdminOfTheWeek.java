package com.staniul.modules.registerers;

import java.io.Serializable;

public class AdminOfTheWeek implements Serializable {
    private int clientDatabaseId;
    private int previousAdminGroup;

    public AdminOfTheWeek(int clientDatabaseId, int previousAdminGroup) {
        this.clientDatabaseId = clientDatabaseId;
        this.previousAdminGroup = previousAdminGroup;
    }

    public int getClientDatabaseId() {
        return clientDatabaseId;
    }

    public int getPreviousAdminGroup() {
        return previousAdminGroup;
    }
}
