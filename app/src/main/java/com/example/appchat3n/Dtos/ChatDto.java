package com.example.appchat3n.Dtos;


public class ChatDto {
    private String chatID;
    private long lastMsgTime;
    public ChatDto() {
    }

    public ChatDto(String chatID, long lastMsgTime) {
        this.chatID = chatID;
        this.lastMsgTime = lastMsgTime;
    }

    public long getLastMsgTime() {
        return lastMsgTime;
    }

    public void setLastMsgTime(long lastMsgTime) {
        this.lastMsgTime = lastMsgTime;
    }

    public String getChatID() {
        return chatID;
    }

    public void setChatID(String chatID) {
        this.chatID = chatID;
    }
}
