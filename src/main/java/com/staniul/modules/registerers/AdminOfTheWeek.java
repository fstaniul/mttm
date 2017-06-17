package com.staniul.modules.registerers;

public class AdminOfTheWeek {
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
