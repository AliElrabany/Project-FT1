package com.iwish.server;

import com.iwish.shared.Message;
import com.iwish.shared.User;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;


public class ClientHandler implements Runnable {

    private Socket socket;
    private Database database;

    public ClientHandler(Socket socket, Database database) {
        this.socket = socket;
        this.database = database;
    }

    @Override
    public void run() {
        try {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            Message request = (Message) in.readObject();
            Message response = handle(request);

            out.writeObject(response);
            out.flush();

            in.close();
            out.close();
            socket.close();
        } catch (Exception e) {
            System.out.println("[Server] Error handling client: " + e.getMessage());
        }
    }

    private Message handle(Message request) {
        Message response = new Message(request.command);
        try {
            switch (request.command) {
                case REGISTER:
                    handleRegister(request, response);
                    break;
                case LOGIN:
                    handleLogin(request, response);
                    break;
                case SEARCH_USERS:
                    handleSearchUsers(request, response);
                    break;
                case ADD_FRIEND:
                    handleAddFriend(request, response);
                    break;
                case GET_FRIEND_REQUESTS:
                    handleGetFriendRequests(request, response);
                    break;
                case ACCEPT_FRIEND_REQUEST:
                    handleRespondFriendRequest(request, response, true);
                    break;
                case DECLINE_FRIEND_REQUEST:
                    handleRespondFriendRequest(request, response, false);
                    break;
                case REMOVE_FRIEND:
                    handleRemoveFriend(request, response);
                    break;
                case GET_FRIENDS:
                    handleGetFriends(request, response);
                    break;
                case GET_CATALOG_ITEMS:
                    handleGetCatalogItems(response);
                    break;
                case GET_MY_WISHLIST:
                    handleGetWishlist(request, response);
                    break;
                case ADD_WISH_ITEM:
                    handleAddWishItem(request, response);
                    break;
                case UPDATE_WISH_ITEM:
                    handleUpdateWishItem(request, response);
                    break;
                case DELETE_WISH_ITEM:
                    handleDeleteWishItem(request, response);
                    break;
                case GET_FRIEND_WISHLIST:
                    handleGetFriendWishlist(request, response);
                    break;
                case CONTRIBUTE:
                    handleContribute(request, response);
                    break;
                case GET_NOTIFICATIONS:
                    handleGetNotifications(request, response);
                    break;
                case MARK_NOTIFICATIONS_READ:
                    handleMarkNotificationsRead(request, response);
                    break;
                default:
                    response.success = false;
                    response.text = "Unknown command.";
            }
        } catch (SQLException e) {
            response.success = false;
            response.text = "Database error: " + e.getMessage();
        }
        return response;
    }

    private void handleRegister(Message request, Message response) throws SQLException {
        String username = request.getString("username");
        String password = request.getString("password");
        String fullName = request.getString("fullName");
        String email = request.getString("email");

        if (username == null || username.trim().length() == 0 || password == null || password.length() == 0) {
            response.success = false;
            response.text = "Username and password are required.";
            return;
        }

        User newUser = database.register(username.trim(), password, fullName, email);
        if (newUser == null) {
            response.success = false;
            response.text = "That username is already taken.";
            return;
        }
        response.success = true;
        response.text = "Account created. Welcome, " + newUser.getFullName() + "!";
        response.put("user", newUser);
    }

    private void handleLogin(Message request, Message response) throws SQLException {
        String username = request.getString("username");
        String password = request.getString("password");

        User user = database.login(username, password);
        if (user == null) {
            response.success = false;
            response.text = "Invalid username or password.";
            return;
        }
        response.success = true;
        response.text = "Welcome back, " + user.getFullName() + "!";
        response.put("user", user);
    }

    private void handleSearchUsers(Message request, Message response) throws SQLException {
        String query = request.getString("query");
        int userId = request.getInt("userId");
        List<User> results = database.searchUsers(query == null ? "" : query, userId);
        response.success = true;
        response.put("users", results);
    }

    private void handleAddFriend(Message request, Message response) throws SQLException {
        int senderId = request.getInt("userId");
        String targetUsername = request.getString("username");
        String error = database.sendFriendRequest(senderId, targetUsername);
        if (error != null) {
            response.success = false;
            response.text = error;
        } else {
            response.success = true;
            response.text = "Friend request sent!";
        }
    }

    private void handleGetFriendRequests(Message request, Message response) throws SQLException {
        int userId = request.getInt("userId");
        response.success = true;
        response.put("requests", database.getFriendRequests(userId));
    }

    private void handleRespondFriendRequest(Message request, Message response, boolean accept) throws SQLException {
        int requestId = request.getInt("requestId");
        int userId = request.getInt("userId");
        String error = database.respondToFriendRequest(requestId, userId, accept);
        if (error != null) {
            response.success = false;
            response.text = error;
        } else {
            response.success = true;
            response.text = accept ? "Friend request accepted." : "Friend request declined.";
        }
    }

    private void handleRemoveFriend(Message request, Message response) throws SQLException {
        int userId = request.getInt("userId");
        int friendId = request.getInt("friendId");
        database.removeFriend(userId, friendId);
        response.success = true;
        response.text = "Friend removed.";
    }

    private void handleGetFriends(Message request, Message response) throws SQLException {
        int userId = request.getInt("userId");
        response.success = true;
        response.put("friends", database.getFriends(userId));
    }

    private void handleGetCatalogItems(Message response) throws SQLException {
        response.success = true;
        response.put("items", database.getCatalogItems());
    }

    private void handleGetWishlist(Message request, Message response) throws SQLException {
        int userId = request.getInt("userId");
        response.success = true;
        response.put("items", database.getWishItems(userId));
    }

    private void handleAddWishItem(Message request, Message response) throws SQLException {
        int userId = request.getInt("userId");
        String name = request.getString("name");
        String description = request.getString("description");
        double price = request.getDouble("price");

        if (name == null || name.trim().length() == 0 || price <= 0) {
            response.success = false;
            response.text = "Item needs a name and a price greater than zero.";
            return;
        }

        response.success = true;
        response.put("item", database.addWishItem(userId, name.trim(), description, price));
    }

    private void handleUpdateWishItem(Message request, Message response) throws SQLException {
        int userId = request.getInt("userId");
        int itemId = request.getInt("itemId");
        String name = request.getString("name");
        String description = request.getString("description");
        double price = request.getDouble("price");

        boolean ok = database.updateWishItem(itemId, userId, name, description, price);
        response.success = ok;
        response.text = ok ? "Item updated." : "Could not update that item.";
    }

    private void handleDeleteWishItem(Message request, Message response) throws SQLException {
        int userId = request.getInt("userId");
        int itemId = request.getInt("itemId");
        boolean ok = database.deleteWishItem(itemId, userId);
        response.success = ok;
        response.text = ok ? "Item deleted." : "Could not delete that item.";
    }

    private void handleGetFriendWishlist(Message request, Message response) throws SQLException {
        int userId = request.getInt("userId");
        int friendId = request.getInt("friendId");

        if (!database.areFriends(userId, friendId)) {
            response.success = false;
            response.text = "You can only view a friend's wish list.";
            return;
        }
        response.success = true;
        response.put("items", database.getWishItems(friendId));
    }

    private void handleContribute(Message request, Message response) throws SQLException {
        int userId = request.getInt("userId");
        int itemId = request.getInt("itemId");
        double amount = request.getDouble("amount");

        String error = database.contribute(itemId, userId, amount);
        if (error != null) {
            response.success = false;
            response.text = error;
        } else {
            response.success = true;
            response.text = "Thanks for contributing $" + amount + "!";
        }
    }

    private void handleGetNotifications(Message request, Message response) throws SQLException {
        int userId = request.getInt("userId");
        response.success = true;
        response.put("notifications", database.getNotifications(userId));
    }

    private void handleMarkNotificationsRead(Message request, Message response) throws SQLException {
        int userId = request.getInt("userId");
        database.markNotificationsRead(userId);
        response.success = true;
    }
}
