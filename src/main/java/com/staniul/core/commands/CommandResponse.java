package com.staniul.core.commands;

public class CommandResponse {
    private CommandExecuteStatus status;
    private String[] response;

    public CommandResponse (String... response) {
        this.status = CommandExecuteStatus.SUCCESSFUL;
        this.response = response;
    }

    public CommandResponse(CommandExecuteStatus status, String... response) {
        this.status = status;
        this.response = response;
    }

    public CommandExecuteStatus getStatus() {
        return status;
    }

    public String[] getResponse() {
        return response;
    }

    public void setResponse(String... response) {
        this.response = response;
    }
}
