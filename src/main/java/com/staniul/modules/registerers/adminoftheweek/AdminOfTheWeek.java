package com.staniul.modules.registerers.adminoftheweek;

import java.io.Serializable;

public class AdminOfTheWeek implements Serializable {
    public static final AdminOfTheWeek NONE = new AdminOfTheWeek(-1, -1);

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

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AdminOfTheWeek &&
                ((AdminOfTheWeek) obj).clientDatabaseId == clientDatabaseId &&
                ((AdminOfTheWeek) obj).previousAdminGroup == previousAdminGroup;
    }

    @Override
    public String toString() {
        return clientDatabaseId + " " + previousAdminGroup;
    }
}
