package com.staniul.teamspeak.modules.channelsmanagers;

public class ChannelManagerException extends Exception {
    private int errorId;

    public ChannelManagerException(int errorId, String message) {
        super(message);
        this.errorId = errorId;
    }

    public ChannelManagerException(int errorId, String message, Throwable cause) {
        super(message, cause);
        this.errorId = errorId;
    }

    public int getErrorId() {
        return errorId;
    }

    @Override
    public String toString() {
        return String.format("Exception %d: %s in %s", errorId, getLocalizedMessage(), getClass().getName());
    }
}
