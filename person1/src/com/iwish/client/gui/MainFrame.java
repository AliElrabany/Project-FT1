package com.iwish.client.gui;

import com.iwish.client.Session;
import com.iwish.shared.User;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private JTabbedPane tabs;
    private FriendsPanel friendsPanel;
    private MyWishlistPanel myWishlistPanel;
    private FriendWishlistPanel friendWishlistPanel;
    private NotificationsPanel notificationsPanel;

    public MainFrame() {
        super("i-Wish - " + Session.currentUser.getFullName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(920, 640);
        setLocationRelativeTo(null);
        setIconImage(IconFactory.createIcon());

        JPanel header = buildHeader();

        tabs = new JTabbedPane();
        tabs.setFont(Theme.BODY_FONT);

        friendWishlistPanel = new FriendWishlistPanel();
        friendsPanel = new FriendsPanel(this);
        myWishlistPanel = new MyWishlistPanel();
        notificationsPanel = new NotificationsPanel();

        tabs.addTab("👥 Friends", friendsPanel);
        tabs.addTab("🎁 My Wish List", myWishlistPanel);
        tabs.addTab("🎀 Friend's Wish List", friendWishlistPanel);
        tabs.addTab("🔔 Notifications", notificationsPanel);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);
        root.add(header, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        JLabel title = new JLabel("i-Wish 🎁");
        title.setFont(Theme.TITLE_FONT);
        title.setForeground(Color.WHITE);

        JLabel welcome = new JLabel("Hi, " + Session.currentUser.getFullName() + "!");
        welcome.setFont(Theme.BODY_FONT);
        welcome.setForeground(Color.WHITE);

        JButton logoutButton = UiHelper.smallButton("Log Out", Theme.PRIMARY_DARK);
        logoutButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                Session.currentUser = null;
                Session.network = null;
                dispose();
                new LoginFrame().setVisible(true);
            }
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);
        right.add(welcome);
        right.add(logoutButton);

        header.add(title, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    /** Called by FriendsPanel when the user clicks "View Wish List" on a friend. */
    public void openFriendWishlist(User friend) {
        friendWishlistPanel.showFriend(friend);
        tabs.setSelectedIndex(2);
    }
}