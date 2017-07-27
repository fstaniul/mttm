package com.staniul.api.security.auth;

public class AuthenticationPostDetails {
    private int id;
    private String uniqueId;
    private String ip;

    public AuthenticationPostDetails() {
    }

    public AuthenticationPostDetails(int id, String uniqueId, String ip) {
        this.id = id;
        this.uniqueId = uniqueId;
        this.ip = ip;
    }

    public int getId() {
        return id;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getIp() {
        return ip;
    }
}
