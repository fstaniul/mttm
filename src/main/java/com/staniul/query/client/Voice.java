package com.staniul.query.client;

public class Voice {
    private boolean connected;
    private boolean muted;

    public Voice(boolean connected, boolean muted) {
        this.connected = connected;
        this.muted = muted;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isMuted() {
        return muted;
    }
}
