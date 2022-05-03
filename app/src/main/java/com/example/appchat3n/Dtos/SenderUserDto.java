package com.example.appchat3n.Dtos;

import com.example.appchat3n.Models.User;

public class SenderUserDto {
    User user;
    long lastMessageTime;


    public SenderUserDto(User user, long lastMessageTime) {
        this.user = user;
        this.lastMessageTime = lastMessageTime;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
}
