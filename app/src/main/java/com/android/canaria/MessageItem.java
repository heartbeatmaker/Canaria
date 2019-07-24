package com.android.canaria;

public class MessageItem {

    private String senderUsername;
    private String message;

    public MessageItem(String senderUsername, String message) {
        this.senderUsername = senderUsername;
        this.message = message;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


}
