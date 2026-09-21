package com.iwish.shared;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A single request or response travelling between client and server.
 *
 * Request example (client -> server):
 *   Message m = new Message(Command.LOGIN);
 *   m.put("username", "ramadan");
 *   m.put("password", "1234");
 *
 * Response example (server -> client):
 *   Message r = new Message(Command.LOGIN);
 *   r.success = true;
 *   r.text = "Welcome back!";
 *   r.put("user", someUserObject);
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    public Command command;
    public Map<String, Object> data;

    public boolean success;
    public String text;

    public Message(Command command) {
        this.command = command;
        this.data = new HashMap<String, Object>();
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public String getString(String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public int getInt(String key) {
        Object value = data.get(key);
        if (value == null) {
            return -1;
        }
        return ((Number) value).intValue();
    }

    public double getDouble(String key) {
        Object value = data.get(key);
        if (value == null) {
            return 0.0;
        }
        return ((Number) value).doubleValue();
    }
}
