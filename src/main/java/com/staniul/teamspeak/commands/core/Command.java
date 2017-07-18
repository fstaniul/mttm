package com.staniul.teamspeak.commands.core;

public class Command {
    private String command;
    private String description;
    private int scope;

    public Command() {
    }

    public Command(String command, String description, int scope) {
        this.command = command;
        this.description = description;
        this.scope = scope;
    }

    public String getCommand() {
        return command;
    }

    public String getDescription() {
        return description;
    }

    public int getScope() {
        return scope;
    }
}
