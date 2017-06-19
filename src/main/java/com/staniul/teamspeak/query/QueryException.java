package com.staniul.teamspeak.query;

/**
 * Exception thrown by Query methods when they finish with error.
 * Most of the time caused by wrong parameter of method (in cause of getting client information that would be
 * non existing client id or requesting client information after client have disconnected).
 * Most of the time this exception is caused by internal jts3serverquery throwing exception because of losing connection
 * with teamspeak 3 server or like in above example a wrong parameter.
 */
public class QueryException extends Exception {
    private int errorId;
    private String errorMsg;

    public QueryException(String message) {
        super(message);
    }

    public QueryException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueryException(int errorId, String errorMsg) {
        super(errorMsg + "(" + errorId + ")");
        this.errorId = errorId;
        this.errorMsg = errorMsg;
    }

    public QueryException(String message, int errorId, String errorMsg) {
        super(message + ", " + errorMsg + "(" + errorId + ")");
        this.errorId = errorId;
        this.errorMsg = errorMsg;
    }

    public QueryException(String message, Throwable cause, int errorId, String errorMsg) {
        super(message + ", " + errorMsg + "(" + errorId + ")", cause);
        this.errorId = errorId;
        this.errorMsg = errorMsg;
    }

    public QueryException(Throwable cause, int errorId, String errorMsg) {
        super(errorMsg + "(" + errorId + ")", cause);
        this.errorId = errorId;
        this.errorMsg = errorMsg;
    }

    @Override
    public String toString() {
        String msg = getLocalizedMessage();
        return String.format("%s: %d, %s. %s", getClass().getName(), errorId, errorMsg, (msg == null ? "" : msg));
    }
}
