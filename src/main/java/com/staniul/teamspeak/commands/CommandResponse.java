package com.staniul.teamspeak.commands;

public class CommandResponse {
    private CommandExecutionStatus status;
    private String message;

    public CommandResponse (String message) {
        status = CommandExecutionStatus.EXECUTED_SUCCESSFULLY;
        this.message = message;
    }

    public CommandResponse(CommandExecutionStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public CommandExecutionStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
