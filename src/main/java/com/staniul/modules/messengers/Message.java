package com.staniul.modules.messengers;

import java.io.Serializable;

public class Message implements Serializable {
    private String message;
    private String owner;

    public Message(String message, String owner) {
        this.message = message;
        this.owner = owner;
    }

    public String getMessage() {
        return message;
    }

    public String getOwner() {
        return owner;
    }

    @Override
    public String toString() {
        return String.format("%s ~[b]%s[/b]", message, owner);
    }
}
