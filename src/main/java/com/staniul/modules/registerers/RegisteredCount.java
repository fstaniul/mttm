package com.staniul.modules.registerers;

import java.io.Serializable;

public class RegisteredCount implements Serializable {
    private int adminDatabaseId;
    private int registerCount;

    public RegisteredCount(int adminDatabaseId, int registerCount) {
        this.adminDatabaseId = adminDatabaseId;
        this.registerCount = registerCount;
    }

    public int getAdminDatabaseId() {
        return adminDatabaseId;
    }

    public int getRegisterCount() {
        return registerCount;
    }

    public void setAdminDatabaseId(int adminDatabaseId) {
        this.adminDatabaseId = adminDatabaseId;
    }

    public void setRegisterCount(int registerCount) {
        this.registerCount = registerCount;
    }
}
