package com.example.appchat3n.Models;

import com.example.appchat3n.Enums.FriendState;

public class Friend {
    private User user;
    private FriendState friendState;

    public Friend() {
    }

    public Friend(User user, FriendState friendState) {
        this.user = user;
        this.friendState = friendState;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public FriendState getFriendState() {
        return friendState;
    }

    public void setFriendState(FriendState friendState) {
        this.friendState = friendState;
    }
}
