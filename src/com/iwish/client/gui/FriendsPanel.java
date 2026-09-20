package com.iwish.client.gui;

import com.iwish.client.Session;
import com.iwish.shared.Command;
import com.iwish.shared.FriendRequest;
import com.iwish.shared.Message;
import com.iwish.shared.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class FriendsPanel extends JPanel {

    private MainFrame mainFrame;
    private JTextField searchField;
    private JPanel requestsList;
    private JPanel friendsList;
    private JLabel statusLabel;

    public FriendsPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        add(buildAddFriendBar(), BorderLayout.NORTH);

        JPanel columns = new JPanel(new GridLayout(1, 2, 20, 0));
        columns.setOpaque(false);

        requestsList = new JPanel();
        requestsList.setLayout(new BoxLayout(requestsList, BoxLayout.Y_AXIS));
        requestsList.setOpaque(false);

        friendsList = new JPanel();
        friendsList.setLayout(new BoxLayout(friendsList, BoxLayout.Y_AXIS));
        friendsList.setOpaque(false);

        columns.add(wrapWithTitle("Friend Requests", requestsList));
        columns.add(wrapWithTitle("My Friends", friendsList));

        add(columns, BorderLayout.CENTER);
        refresh();
    }

    private JPanel buildAddFriendBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        searchField = UiHelper.field();
        JButton addButton = UiHelper.primaryButton("Send Friend Request");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendFriendRequest();
            }
        });
        searchField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendFriendRequest();
            }
        });

        statusLabel = new JLabel(" ");
        statusLabel.setFont(Theme.SMALL_FONT);

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        top.add(searchField, BorderLayout.CENTER);
        top.add(addButton, BorderLayout.EAST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(top, BorderLayout.NORTH);
        wrapper.add(statusLabel, BorderLayout.SOUTH);
        bar.add(wrapper, BorderLayout.CENTER);
        return bar;
    }

    private JPanel wrapWithTitle(String title, JPanel content) {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);
        outer.add(UiHelper.heading(title), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }

    private void sendFriendRequest() {
        String username = searchField.getText().trim();
        if (username.length() == 0) {
            statusLabel.setText("Type a username first.");
            return;
        }
        Message request = new Message(Command.ADD_FRIEND);
        request.put("userId", Session.currentUser.getId());
        request.put("username", username);
        try {
            Message response = Session.network.send(request);
            statusLabel.setForeground(response.success ? Theme.SUCCESS : Theme.ACCENT);
            statusLabel.setText(response.text);
            if (response.success) {
                searchField.setText("");
            }
        } catch (Exception ex) {
            statusLabel.setForeground(Theme.ACCENT);
            statusLabel.setText("Network error: " + ex.getMessage());
        }
    }

    public void refresh() {
        loadFriendRequests();
        loadFriends();
    }

    private void loadFriendRequests() {
        requestsList.removeAll();
        try {
            Message request = new Message(Command.GET_FRIEND_REQUESTS);
            request.put("userId", Session.currentUser.getId());
            Message response = Session.network.send(request);

            @SuppressWarnings("unchecked")
            List<FriendRequest> requests = (List<FriendRequest>) response.get("requests");

            if (requests == null || requests.isEmpty()) {
                requestsList.add(UiHelper.muted("No pending requests."));
            } else {
                for (int i = 0; i < requests.size(); i++) {
                    requestsList.add(buildRequestRow(requests.get(i)));
                    requestsList.add(Box.createVerticalStrut(8));
                }
            }
        } catch (Exception ex) {
            requestsList.add(UiHelper.muted("Could not load requests: " + ex.getMessage()));
        }
        requestsList.revalidate();
        requestsList.repaint();
    }

    private JPanel buildRequestRow(final FriendRequest req) {
        JPanel row = card();
        JLabel name = new JLabel(req.getSenderFullName() + " (@" + req.getSenderUsername() + ")");
        name.setFont(Theme.BODY_FONT);

        JButton accept = UiHelper.smallButton("Accept", Theme.SUCCESS);
        JButton decline = UiHelper.smallButton("Decline", Theme.ACCENT);

        accept.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                respond(req.getId(), true);
            }
        });
        decline.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                respond(req.getId(), false);
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(accept);
        buttons.add(decline);

        row.add(name, BorderLayout.NORTH);
        row.add(buttons, BorderLayout.SOUTH);
        return row;
    }

    private void respond(int requestId, boolean accept) {
        Message request = new Message(accept ? Command.ACCEPT_FRIEND_REQUEST : Command.DECLINE_FRIEND_REQUEST);
        request.put("userId", Session.currentUser.getId());
        request.put("requestId", requestId);
        try {
            Session.network.send(request);
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Network error: " + ex.getMessage());
        }
    }

    private void loadFriends() {
        friendsList.removeAll();
        try {
            Message request = new Message(Command.GET_FRIENDS);
            request.put("userId", Session.currentUser.getId());
            Message response = Session.network.send(request);

            @SuppressWarnings("unchecked")
            List<User> friends = (List<User>) response.get("friends");

            if (friends == null || friends.isEmpty()) {
                friendsList.add(UiHelper.muted("No friends yet - send a request above!"));
            } else {
                for (int i = 0; i < friends.size(); i++) {
                    friendsList.add(buildFriendRow(friends.get(i)));
                    friendsList.add(Box.createVerticalStrut(8));
                }
            }
        } catch (Exception ex) {
            friendsList.add(UiHelper.muted("Could not load friends: " + ex.getMessage()));
        }
        friendsList.revalidate();
        friendsList.repaint();
    }

    private JPanel buildFriendRow(final User friend) {
        JPanel row = card();
        JLabel name = new JLabel(friend.getFullName() + " (@" + friend.getUsername() + ")");
        name.setFont(Theme.BODY_FONT);

        JButton view = UiHelper.smallButton("View Wish List", Theme.PRIMARY);
        JButton remove = UiHelper.smallButton("Remove", Theme.MUTED);

        view.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainFrame.openFriendWishlist(friend);
            }
        });
        remove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeFriend(friend);
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(view);
        buttons.add(remove);

        row.add(name, BorderLayout.NORTH);
        row.add(buttons, BorderLayout.SOUTH);
        return row;
    }

    private void removeFriend(User friend) {
        int confirm = JOptionPane.showConfirmDialog(this, "Remove " + friend.getFullName() + " as a friend?",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        Message request = new Message(Command.REMOVE_FRIEND);
        request.put("userId", Session.currentUser.getId());
        request.put("friendId", friend.getId());
        try {
            Session.network.send(request);
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Network error: " + ex.getMessage());
        }
    }

    private JPanel card() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Theme.CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }
}