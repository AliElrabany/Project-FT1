package com.iwish.shared;

import java.io.Serializable;

public class FriendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int senderId;
    private String senderUsername;
    private String senderFullName;

    public FriendRequest(int id, int senderId, String senderUsername, String senderFullName) {
        this.id = id;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.senderFullName = senderFullName;
    }

    public int getId() {
        return id;
    }

    public int getSenderId() {
        return senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getSenderFullName() {
        return senderFullName;
    }
}
