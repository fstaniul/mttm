package com.staniul.modules.registerers.adminoftheweek;

public class AdminInfo {
    private int id;
    private int group;
    private int registeredCount;

    public AdminInfo(int id, int group) {
        this.id = id;
        this.group = group;
        registeredCount = 0;
    }

    public int getId() {
        return id;
    }

    public int getGroup() {
        return group;
    }

    public int getRegisteredCount() {
        return registeredCount;
    }

    public void addRegistered (int count) {
        registeredCount += count;
    }
}
