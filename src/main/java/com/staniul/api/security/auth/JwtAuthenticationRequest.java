package com.staniul.api.security.auth;

/**
 * Authentication request that should be send to server in a json format:
 * {
 *      "id": "123123",
 *      "uniqueId": "13da123141da/1231241da=",
 *      "ip": "123.123.123"
 * }
 */
public class JwtAuthenticationRequest {
    private int id;
    private String uniqueId;
    private String ip;

    public JwtAuthenticationRequest() {
    }

    public JwtAuthenticationRequest(int id, String uniqueId, String ip) {
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
