package com.iwish.server;

import com.iwish.shared.CatalogItem;
import com.iwish.shared.FriendRequest;
import com.iwish.shared.Notification;
import com.iwish.shared.User;
import com.iwish.shared.WishItem;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Everything about talking to the MySQL database lives in this one class.
 * The rest of the server only ever calls methods here - it never writes SQL itself.
 *
 * All methods are synchronized because a single MySQL connection is shared
 * by every client-handling thread; this keeps the project simple and correct
 * without needing a connection pool.
 */
public class Database {

    private Connection connection;

    public Database(String dbName) throws SQLException {
        this(dbName, "localhost", 3306, "root", "4/7/2004Ram");
    }

    public Database(String dbName, String host, int port, String user, String password) throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found on the classpath. " +
                "Make sure mysql-connector-j.jar is added as a library.", e);
        }
        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName +
            "?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true";
        connection = DriverManager.getConnection(url, user, password);
        createTables();
        seedCatalogIfEmpty();
    }

    private void createTables() throws SQLException {
        Statement statement = connection.createStatement();

        statement.execute(
            "CREATE TABLE IF NOT EXISTS users (" +
            "id INT AUTO_INCREMENT PRIMARY KEY," +
            "username VARCHAR(100) UNIQUE NOT NULL," +
            "password VARCHAR(255) NOT NULL," +
            "full_name VARCHAR(150) NOT NULL," +
            "email VARCHAR(150)" +
            ")"
        );

        statement.execute(
            "CREATE TABLE IF NOT EXISTS catalog_items (" +
            "id INT AUTO_INCREMENT PRIMARY KEY," +
            "name VARCHAR(150) NOT NULL," +
            "description TEXT," +
            "price DECIMAL(10,2) NOT NULL" +
            ")"
        );

        statement.execute(
            "CREATE TABLE IF NOT EXISTS wish_items (" +
            "id INT AUTO_INCREMENT PRIMARY KEY," +
            "owner_id INT NOT NULL," +
            "name VARCHAR(150) NOT NULL," +
            "description TEXT," +
            "price DECIMAL(10,2) NOT NULL," +
            "amount_contributed DECIMAL(10,2) NOT NULL DEFAULT 0," +
            "fulfilled TINYINT(1) NOT NULL DEFAULT 0," +
            "FOREIGN KEY(owner_id) REFERENCES users(id)" +
            ")"
        );

        statement.execute(
            "CREATE TABLE IF NOT EXISTS friend_requests (" +
            "id INT AUTO_INCREMENT PRIMARY KEY," +
            "sender_id INT NOT NULL," +
            "receiver_id INT NOT NULL," +
            "status VARCHAR(20) NOT NULL DEFAULT 'PENDING'," +
            "FOREIGN KEY(sender_id) REFERENCES users(id)," +
            "FOREIGN KEY(receiver_id) REFERENCES users(id)" +
            ")"
        );

        statement.execute(
            "CREATE TABLE IF NOT EXISTS friends (" +
            "id INT AUTO_INCREMENT PRIMARY KEY," +
            "user_id INT NOT NULL," +
            "friend_id INT NOT NULL," +
            "FOREIGN KEY(user_id) REFERENCES users(id)," +
            "FOREIGN KEY(friend_id) REFERENCES users(id)" +
            ")"
        );

        statement.execute(
            "CREATE TABLE IF NOT EXISTS contributions (" +
            "id INT AUTO_INCREMENT PRIMARY KEY," +
            "wish_item_id INT NOT NULL," +
            "contributor_id INT NOT NULL," +
            "amount DECIMAL(10,2) NOT NULL," +
            "created_at DATETIME NOT NULL," +
            "FOREIGN KEY(wish_item_id) REFERENCES wish_items(id)," +
            "FOREIGN KEY(contributor_id) REFERENCES users(id)" +
            ")"
        );

        statement.execute(
            "CREATE TABLE IF NOT EXISTS notifications (" +
            "id INT AUTO_INCREMENT PRIMARY KEY," +
            "user_id INT NOT NULL," +
            "message VARCHAR(500) NOT NULL," +
            "is_read TINYINT(1) NOT NULL DEFAULT 0," +
            "created_at DATETIME NOT NULL," +
            "FOREIGN KEY(user_id) REFERENCES users(id)" +
            ")"
        );

        statement.close();
    }

    /** Seeds a handful of catalog items on first run, like an admin would. */
    private void seedCatalogIfEmpty() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT COUNT(*) AS c FROM catalog_items");
        int count = 0;
        if (rs.next()) {
            count = rs.getInt("c");
        }
        rs.close();
        statement.close();

        if (count > 0) {
            return;
        }

        Object[][] seed = {
                {"RTX 5090 Ti", "High-end graphics card", 599.0},
                {"RAM 16GB RGB", "16GB RGB desktop memory kit", 400.0},
                {"AirPods", "Wireless earbuds", 200.0},
                {"Smart Watch", "Fitness & notifications smartwatch", 300.0},
                {"FIFA 2027", "Latest football video game", 69.0},
                {"GTA 6", "Open-world action video game", 120.0}
        };

        PreparedStatement insert = connection.prepareStatement(
            "INSERT INTO catalog_items(name, description, price) VALUES(?,?,?)"
        );
        for (int i = 0; i < seed.length; i++) {
            insert.setString(1, (String) seed[i][0]);
            insert.setString(2, (String) seed[i][1]);
            insert.setDouble(3, (Double) seed[i][2]);
            insert.executeUpdate();
        }
        insert.close();
    }

    private String now() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(new Date());
    }


    public synchronized User register(String username, String password, String fullName, String email) throws SQLException {
        PreparedStatement check = connection.prepareStatement("SELECT id FROM users WHERE username = ?");
        check.setString(1, username);
        ResultSet rs = check.executeQuery();
        boolean taken = rs.next();
        rs.close();
        check.close();
        if (taken) {
            return null;
        }

        PreparedStatement insert = connection.prepareStatement(
            "INSERT INTO users(username, password, full_name, email) VALUES(?,?,?,?)"
        );
        insert.setString(1, username);
        insert.setString(2, password);
        insert.setString(3, fullName);
        insert.setString(4, email);
        insert.executeUpdate();

        PreparedStatement idQuery = connection.prepareStatement("SELECT LAST_INSERT_ID() AS id");
        ResultSet idRs = idQuery.executeQuery();
        int newId = -1;
        if (idRs.next()) {
            newId = idRs.getInt("id");
        }
        idRs.close();
        idQuery.close();
        insert.close();

        return new User(newId, username, fullName, email);
    }

    public synchronized User login(String username, String password) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
            "SELECT id, username, full_name, email FROM users WHERE username = ? AND password = ?"
        );
        statement.setString(1, username);
        statement.setString(2, password);
        ResultSet rs = statement.executeQuery();
        User user = null;
        if (rs.next()) {
            user = new User(rs.getInt("id"), rs.getString("username"), rs.getString("full_name"), rs.getString("email"));
        }
        rs.close();
        statement.close();
        return user;
    }

    public synchronized List<User> searchUsers(String query, int excludeUserId) throws SQLException {
        List<User> results = new ArrayList<User>();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT id, username, full_name, email FROM users " +
            "WHERE (username LIKE ? OR full_name LIKE ?) AND id != ?"
        );
        String like = "%" + query + "%";
        statement.setString(1, like);
        statement.setString(2, like);
        statement.setInt(3, excludeUserId);
        ResultSet rs = statement.executeQuery();
        while (rs.next()) {
            results.add(new User(rs.getInt("id"), rs.getString("username"), rs.getString("full_name"), rs.getString("email")));
        }
        rs.close();
        statement.close();
        return results;
    }

    private User findUserByUsername(String username) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
            "SELECT id, username, full_name, email FROM users WHERE username = ?"
        );
        statement.setString(1, username);
        ResultSet rs = statement.executeQuery();
        User user = null;
        if (rs.next()) {
            user = new User(rs.getInt("id"), rs.getString("username"), rs.getString("full_name"), rs.getString("email"));
        }
        rs.close();
        statement.close();
        return user;
    }



    public synchronized boolean areFriends(int userId, int otherId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
            "SELECT id FROM friends WHERE user_id = ? AND friend_id = ?"
        );
        statement.setInt(1, userId);
        statement.setInt(2, otherId);
        ResultSet rs = statement.executeQuery();
        boolean areFriends = rs.next();
        rs.close();
        statement.close();
        return areFriends;
    }

    /** Returns null on success, or an error message describing why the request could not be sent. */
    public synchronized String sendFriendRequest(int senderId, String receiverUsername) throws SQLException {
        User receiver = findUserByUsername(receiverUsername);
        if (receiver == null) {
            return "No user with that username exists.";
        }
        if (receiver.getId() == senderId) {
            return "You can't add yourself as a friend.";
        }
        if (areFriends(senderId, receiver.getId())) {
            return "You are already friends.";
        }

        PreparedStatement dup = connection.prepareStatement(
            "SELECT id FROM friend_requests WHERE sender_id = ? AND receiver_id = ? AND status = 'PENDING'"
        );
        dup.setInt(1, senderId);
        dup.setInt(2, receiver.getId());
        ResultSet dupRs = dup.executeQuery();
        boolean alreadySent = dupRs.next();
        dupRs.close();
        dup.close();
        if (alreadySent) {
            return "You already sent a request to this user.";
        }

        PreparedStatement insert = connection.prepareStatement(
            "INSERT INTO friend_requests(sender_id, receiver_id, status) VALUES(?,?,'PENDING')"
        );
        insert.setInt(1, senderId);
        insert.setInt(2, receiver.getId());
        insert.executeUpdate();
        insert.close();

        User sender = findUserById(senderId);
        addNotification(receiver.getId(), sender.getFullName() + " sent you a friend request.");

        return null;
    }

    private User findUserById(int id) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
            "SELECT id, username, full_name, email FROM users WHERE id = ?"
        );
        statement.setInt(1, id);
        ResultSet rs = statement.executeQuery();
        User user = null;
        if (rs.next()) {
            user = new User(rs.getInt("id"), rs.getString("username"), rs.getString("full_name"), rs.getString("email"));
        }
        rs.close();
        statement.close();
        return user;
    }

    public synchronized List<FriendRequest> getFriendRequests(int userId) throws SQLException {
        List<FriendRequest> results = new ArrayList<FriendRequest>();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT fr.id, u.id AS sender_id, u.username, u.full_name " +
            "FROM friend_requests fr JOIN users u ON fr.sender_id = u.id " +
            "WHERE fr.receiver_id = ? AND fr.status = 'PENDING'"
        );
        statement.setInt(1, userId);
        ResultSet rs = statement.executeQuery();
        while (rs.next()) {
            results.add(new FriendRequest(
                rs.getInt("id"), rs.getInt("sender_id"), rs.getString("username"), rs.getString("full_name")
            ));
        }
        rs.close();
        statement.close();
        return results;
    }

    public synchronized String respondToFriendRequest(int requestId, int receiverId, boolean accept) throws SQLException {
        PreparedStatement find = connection.prepareStatement(
            "SELECT sender_id, receiver_id, status FROM friend_requests WHERE id = ?"
        );
        find.setInt(1, requestId);
        ResultSet rs = find.executeQuery();
        if (!rs.next()) {
            rs.close();
            find.close();
            return "That friend request no longer exists.";
        }
        int senderId = rs.getInt("sender_id");
        int receiverIdInRow = rs.getInt("receiver_id");
        String status = rs.getString("status");
        rs.close();
        find.close();

        if (receiverIdInRow != receiverId) {
            return "This request does not belong to you.";
        }
        if (!"PENDING".equals(status)) {
            return "This request was already answered.";
        }

        String newStatus = accept ? "ACCEPTED" : "DECLINED";
        PreparedStatement update = connection.prepareStatement("UPDATE friend_requests SET status = ? WHERE id = ?");
        update.setString(1, newStatus);
        update.setInt(2, requestId);
        update.executeUpdate();
        update.close();

        if (accept) {
            PreparedStatement addFriendship = connection.prepareStatement(
                "INSERT INTO friends(user_id, friend_id) VALUES(?,?)"
            );
            addFriendship.setInt(1, senderId);
            addFriendship.setInt(2, receiverId);
            addFriendship.executeUpdate();
            addFriendship.setInt(1, receiverId);
            addFriendship.setInt(2, senderId);
            addFriendship.executeUpdate();
            addFriendship.close();

            User receiver = findUserById(receiverId);
            addNotification(senderId, receiver.getFullName() + " accepted your friend request.");
        }

        return null;
    }

    public synchronized void removeFriend(int userId, int friendId) throws SQLException {
        PreparedStatement delete = connection.prepareStatement(
            "DELETE FROM friends WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)"
        );
        delete.setInt(1, userId);
        delete.setInt(2, friendId);
        delete.setInt(3, friendId);
        delete.setInt(4, userId);
        delete.executeUpdate();
        delete.close();
    }

    public synchronized List<User> getFriends(int userId) throws SQLException {
        List<User> results = new ArrayList<User>();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT u.id, u.username, u.full_name, u.email FROM friends f " +
            "JOIN users u ON f.friend_id = u.id WHERE f.user_id = ?"
        );
        statement.setInt(1, userId);
        ResultSet rs = statement.executeQuery();
        while (rs.next()) {
            results.add(new User(rs.getInt("id"), rs.getString("username"), rs.getString("full_name"), rs.getString("email")));
        }
        rs.close();
        statement.close();
        return results;
    }


    public synchronized List<CatalogItem> getCatalogItems() throws SQLException {
        List<CatalogItem> results = new ArrayList<CatalogItem>();
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT id, name, description, price FROM catalog_items ORDER BY name");
        while (rs.next()) {
            results.add(new CatalogItem(rs.getInt("id"), rs.getString("name"), rs.getString("description"), rs.getDouble("price")));
        }
        rs.close();
        statement.close();
        return results;
    }

    public synchronized List<WishItem> getWishItems(int ownerId) throws SQLException {
        List<WishItem> results = new ArrayList<WishItem>();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT id, owner_id, name, description, price, amount_contributed, fulfilled " +
            "FROM wish_items WHERE owner_id = ? ORDER BY fulfilled ASC, id DESC"
        );
        statement.setInt(1, ownerId);
        ResultSet rs = statement.executeQuery();
        while (rs.next()) {
            results.add(new WishItem(
                rs.getInt("id"), rs.getInt("owner_id"), rs.getString("name"), rs.getString("description"),
                rs.getDouble("price"), rs.getDouble("amount_contributed"), rs.getInt("fulfilled") == 1
            ));
        }
        rs.close();
        statement.close();
        return results;
    }

    public synchronized WishItem addWishItem(int ownerId, String name, String description, double price) throws SQLException {
        PreparedStatement insert = connection.prepareStatement(
            "INSERT INTO wish_items(owner_id, name, description, price, amount_contributed, fulfilled) VALUES(?,?,?,?,0,0)"
        );
        insert.setInt(1, ownerId);
        insert.setString(2, name);
        insert.setString(3, description);
        insert.setDouble(4, price);
        insert.executeUpdate();
        insert.close();

        Statement idQuery = connection.createStatement();
        ResultSet idRs = idQuery.executeQuery("SELECT LAST_INSERT_ID() AS id");
        int newId = -1;
        if (idRs.next()) {
            newId = idRs.getInt("id");
        }
        idRs.close();
        idQuery.close();

        return new WishItem(newId, ownerId, name, description, price, 0, false);
    }

    public synchronized boolean updateWishItem(int itemId, int ownerId, String name, String description, double price) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
            "UPDATE wish_items SET name = ?, description = ?, price = ? WHERE id = ? AND owner_id = ?"
        );
        statement.setString(1, name);
        statement.setString(2, description);
        statement.setDouble(3, price);
        statement.setInt(4, itemId);
        statement.setInt(5, ownerId);
        int rows = statement.executeUpdate();
        statement.close();
        return rows > 0;
    }

    public synchronized boolean deleteWishItem(int itemId, int ownerId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
            "DELETE FROM wish_items WHERE id = ? AND owner_id = ?"
        );
        statement.setInt(1, itemId);
        statement.setInt(2, ownerId);
        int rows = statement.executeUpdate();
        statement.close();
        return rows > 0;
    }

    private WishItem findWishItem(int itemId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
            "SELECT id, owner_id, name, description, price, amount_contributed, fulfilled FROM wish_items WHERE id = ?"
        );
        statement.setInt(1, itemId);
        ResultSet rs = statement.executeQuery();
        WishItem item = null;
        if (rs.next()) {
            item = new WishItem(
                rs.getInt("id"), rs.getInt("owner_id"), rs.getString("name"), rs.getString("description"),
                rs.getDouble("price"), rs.getDouble("amount_contributed"), rs.getInt("fulfilled") == 1
            );
        }
        rs.close();
        statement.close();
        return item;
    }

    public synchronized String contribute(int wishItemId, int contributorId, double amount) throws SQLException {
        if (amount <= 0) {
            return "Contribution amount must be greater than zero.";
        }

        WishItem item = findWishItem(wishItemId);
        if (item == null) {
            return "That wish list item no longer exists.";
        }
        if (item.getOwnerId() == contributorId) {
            return "You can't contribute to your own wish list item.";
        }
        if (!areFriends(contributorId, item.getOwnerId())) {
            return "You can only contribute to a friend's wish list.";
        }
        if (item.isFulfilled()) {
            return "This item is already fully funded.";
        }

        double newAmount = item.getAmountContributed() + amount;
        boolean nowFulfilled = newAmount >= item.getPrice();
        if (nowFulfilled) {
            newAmount = item.getPrice();
        }

        PreparedStatement update = connection.prepareStatement(
            "UPDATE wish_items SET amount_contributed = ?, fulfilled = ? WHERE id = ?"
        );
        update.setDouble(1, newAmount);
        update.setInt(2, nowFulfilled ? 1 : 0);
        update.setInt(3, wishItemId);
        update.executeUpdate();
        update.close();

        PreparedStatement insert = connection.prepareStatement(
            "INSERT INTO contributions(wish_item_id, contributor_id, amount, created_at) VALUES(?,?,?,?)"
        );
        insert.setInt(1, wishItemId);
        insert.setInt(2, contributorId);
        insert.setDouble(3, amount);
        insert.setString(4, now());
        insert.executeUpdate();
        insert.close();

        User contributor = findUserById(contributorId);
        User owner = findUserById(item.getOwnerId());

        addNotification(item.getOwnerId(), contributor.getFullName() + " contributed $" + amount + " towards \"" + item.getName() + "\".");

        if (nowFulfilled) {
            addNotification(contributorId, "\"" + item.getName() + "\" is now fully funded! " + owner.getFullName() + " will love it.");
            List<Integer> otherContributors = getDistinctContributors(wishItemId, contributorId);
            for (int i = 0; i < otherContributors.size(); i++) {
                addNotification(otherContributors.get(i), "\"" + item.getName() + "\" is now fully funded!");
            }
            addNotification(item.getOwnerId(), "\"" + item.getName() + "\" on your wish list is now fully funded!");
        }

        return null;
    }

    private List<Integer> getDistinctContributors(int wishItemId, int excludeUserId) throws SQLException {
        List<Integer> results = new ArrayList<Integer>();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT DISTINCT contributor_id FROM contributions WHERE wish_item_id = ? AND contributor_id != ?"
        );
        statement.setInt(1, wishItemId);
        statement.setInt(2, excludeUserId);
        ResultSet rs = statement.executeQuery();
        while (rs.next()) {
            results.add(rs.getInt("contributor_id"));
        }
        rs.close();
        statement.close();
        return results;
    }


    public synchronized void addNotification(int userId, String message) throws SQLException {
        PreparedStatement insert = connection.prepareStatement(
            "INSERT INTO notifications(user_id, message, is_read, created_at) VALUES(?,?,0,?)"
        );
        insert.setInt(1, userId);
        insert.setString(2, message);
        insert.setString(3, now());
        insert.executeUpdate();
        insert.close();
    }

    public synchronized List<Notification> getNotifications(int userId) throws SQLException {
        List<Notification> results = new ArrayList<Notification>();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT id, message, is_read, created_at FROM notifications WHERE user_id = ? ORDER BY id DESC"
        );
        statement.setInt(1, userId);
        ResultSet rs = statement.executeQuery();
        while (rs.next()) {
            results.add(new Notification(rs.getInt("id"), rs.getString("message"), rs.getInt("is_read") == 1, rs.getString("created_at")));
        }
        rs.close();
        statement.close();
        return results;
    }

    public synchronized void markNotificationsRead(int userId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
            "UPDATE notifications SET is_read = 1 WHERE user_id = ?"
        );
        statement.setInt(1, userId);
        statement.executeUpdate();
        statement.close();
    }
}
