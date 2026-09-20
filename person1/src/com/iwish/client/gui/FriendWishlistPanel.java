package com.iwish.client.gui;

import com.iwish.client.Session;
import com.iwish.shared.Command;
import com.iwish.shared.Message;
import com.iwish.shared.User;
import com.iwish.shared.WishItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class FriendWishlistPanel extends JPanel {

    private JComboBox<User> friendCombo;
    private JPanel itemsList;
    private boolean loadingCombo = false;

    public FriendWishlistPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        top.add(UiHelper.heading("Friend's Wish List"), BorderLayout.WEST);

        friendCombo = new JComboBox<User>();
        friendCombo.setPreferredSize(new Dimension(260, 32));
        friendCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!loadingCombo) {
                    loadWishlist();
                }
            }
        });
        top.add(friendCombo, BorderLayout.EAST);

        itemsList = new JPanel();
        itemsList.setLayout(new BoxLayout(itemsList, BoxLayout.Y_AXIS));
        itemsList.setOpaque(false);

        JScrollPane scroll = new JScrollPane(itemsList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(14);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        loadFriends(null);
    }

    /** Called when the user clicks "View Wish List" on the Friends tab. */
    public void showFriend(User friend) {
        loadFriends(friend);
    }

    private void loadFriends(User preselect) {
        try {
            Message request = new Message(Command.GET_FRIENDS);
            request.put("userId", Session.currentUser.getId());
            Message response = Session.network.send(request);

            @SuppressWarnings("unchecked")
            List<User> friends = (List<User>) response.get("friends");

            loadingCombo = true;
            friendCombo.removeAllItems();
            int selectIndex = -1;
            if (friends != null) {
                for (int i = 0; i < friends.size(); i++) {
                    friendCombo.addItem(friends.get(i));
                    if (preselect != null && friends.get(i).getId() == preselect.getId()) {
                        selectIndex = i;
                    }
                }
            }
            loadingCombo = false;

            if (selectIndex >= 0) {
                friendCombo.setSelectedIndex(selectIndex);
            } else if (friendCombo.getItemCount() > 0) {
                friendCombo.setSelectedIndex(0);
            }
            loadWishlist();
        } catch (Exception ex) {
            itemsList.removeAll();
            itemsList.add(UiHelper.muted("Could not load friends: " + ex.getMessage()));
            itemsList.revalidate();
            itemsList.repaint();
        }
    }

    private void loadWishlist() {
        itemsList.removeAll();
        User friend = (User) friendCombo.getSelectedItem();

        if (friend == null) {
            itemsList.add(UiHelper.muted("Add some friends first, then pick one here to see their wish list!"));
            itemsList.revalidate();
            itemsList.repaint();
            return;
        }

        try {
            Message request = new Message(Command.GET_FRIEND_WISHLIST);
            request.put("userId", Session.currentUser.getId());
            request.put("friendId", friend.getId());
            Message response = Session.network.send(request);

            if (!response.success) {
                itemsList.add(UiHelper.muted(response.text));
            } else {
                @SuppressWarnings("unchecked")
                List<WishItem> items = (List<WishItem>) response.get("items");
                if (items == null || items.isEmpty()) {
                    itemsList.add(UiHelper.muted(friend.getFullName() + " hasn't added anything to their wish list yet."));
                } else {
                    for (int i = 0; i < items.size(); i++) {
                        itemsList.add(buildItemRow(items.get(i)));
                        itemsList.add(Box.createVerticalStrut(10));
                    }
                }
            }
        } catch (Exception ex) {
            itemsList.add(UiHelper.muted("Could not load wish list: " + ex.getMessage()));
        }
        itemsList.revalidate();
        itemsList.repaint();
    }

    private JPanel buildItemRow(final WishItem item) {
        JPanel row = new JPanel(new BorderLayout(0, 8));
        row.setBackground(Theme.CARD);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER, 1, true),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel name = new JLabel((item.isFulfilled() ? "✅ " : "🎁 ") + item.getName() + "   ·   $" + item.getPrice());
        name.setFont(Theme.BODY_FONT);
        JLabel description = UiHelper.muted(item.getDescription() == null ? "" : item.getDescription());

        JPanel top = new JPanel(new GridLayout(2, 1));
        top.setOpaque(false);
        top.add(name);
        top.add(description);

        JProgressBar progress = new JProgressBar(0, 100);
        progress.setValue(item.getProgressPercent());
        progress.setStringPainted(true);
        progress.setForeground(item.isFulfilled() ? Theme.SUCCESS : Theme.PRIMARY);

        JButton contributeButton = UiHelper.smallButton(item.isFulfilled() ? "Fully Funded" : "Contribute", Theme.PRIMARY);
        contributeButton.setEnabled(!item.isFulfilled());
        contributeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ContributeDialog dialog = new ContributeDialog((Frame) SwingUtilities.getWindowAncestor(FriendWishlistPanel.this), item);
                dialog.setVisible(true);
                if (dialog.didContribute()) {
                    loadWishlist();
                }
            }
        });

        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setOpaque(false);
        bottom.add(progress, BorderLayout.CENTER);
        bottom.add(contributeButton, BorderLayout.EAST);

        row.add(top, BorderLayout.NORTH);
        row.add(bottom, BorderLayout.SOUTH);
        return row;
    }
}
