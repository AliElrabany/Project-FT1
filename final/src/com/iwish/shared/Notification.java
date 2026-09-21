package com.iwish.shared;

import java.io.Serializable;

public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String message;
    private boolean read;
    private String createdAt;

    public Notification(int id, String message, boolean read, String createdAt) {
        this.id = id;
        this.message = message;
        this.read = read;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
