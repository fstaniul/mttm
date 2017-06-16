package com.staniul.teamspeak.commands;

import com.staniul.util.lang.StringUtil;

/**
 * <p>Class that represents a command response. Each command should return this class object.</p>
 *
 * <p>Used by {@link CommandMessengerAspect} to send response to client after command execution, whether it was successful or
 * not.</p>
 */
public class CommandResponse {
    private CommandExecutionStatus status;
    private String[] message;

    public CommandResponse() {
        status = CommandExecutionStatus.EXECUTED_SUCCESSFULLY;
        this.message = null;
    }

    public CommandResponse(String message) {
        status = CommandExecutionStatus.EXECUTED_SUCCESSFULLY;
        this.message = StringUtil.splitOnSize(message, " ", 512);
    }

    public CommandResponse(String[] message) {
        status = CommandExecutionStatus.EXECUTED_SUCCESSFULLY;
        this.message = message;
    }

    public CommandResponse(CommandExecutionStatus status, String[] message) {
        this.status = status;
        this.message = message;
    }

    public CommandExecutionStatus getStatus() {
        return status;
    }

    public String[] getMessage() {
        return message;
    }

    public void setMessage(String[] message) {
        this.message = message;
    }
}
