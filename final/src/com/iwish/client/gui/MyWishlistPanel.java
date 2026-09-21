package com.iwish.client.gui;

import com.iwish.client.Session;
import com.iwish.shared.Command;
import com.iwish.shared.Message;
import com.iwish.shared.WishItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class MyWishlistPanel extends JPanel {

    private JPanel itemsList;

    public MyWishlistPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        top.add(UiHelper.heading("My Wish List"), BorderLayout.WEST);

        JButton addButton = UiHelper.primaryButton("+ Add Item");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AddEditItemDialog dialog = new AddEditItemDialog((Frame) SwingUtilities.getWindowAncestor(MyWishlistPanel.this), null);
                dialog.setVisible(true);
                if (dialog.wasSaved()) {
                    refresh();
                }
            }
        });
        top.add(addButton, BorderLayout.EAST);

        itemsList = new JPanel();
        itemsList.setLayout(new BoxLayout(itemsList, BoxLayout.Y_AXIS));
        itemsList.setOpaque(false);

        JScrollPane scroll = new JScrollPane(itemsList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(14);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        refresh();
    }

    public void refresh() {
        itemsList.removeAll();
        try {
            Message request = new Message(Command.GET_MY_WISHLIST);
            request.put("userId", Session.currentUser.getId());
            Message response = Session.network.send(request);

            @SuppressWarnings("unchecked")
            List<WishItem> items = (List<WishItem>) response.get("items");

            if (items == null || items.isEmpty()) {
                itemsList.add(UiHelper.muted("Your wish list is empty - click \"+ Add Item\" to get started."));
            } else {
                for (int i = 0; i < items.size(); i++) {
                    itemsList.add(buildItemRow(items.get(i)));
                    itemsList.add(Box.createVerticalStrut(10));
                }
            }
        } catch (Exception ex) {
            itemsList.add(UiHelper.muted("Could not load your wish list: " + ex.getMessage()));
        }
        itemsList.revalidate();
        itemsList.repaint();
    }

    private JPanel buildItemRow(final WishItem item) {
        JPanel row = new JPanel();
        row.setLayout(new BorderLayout(0, 8));
        row.setBackground(Theme.CARD);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER, 1, true),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        String priceLine = "$" + item.getPrice() + "   ·   $" + item.getAmountContributed() + " contributed so far";
        JLabel name = new JLabel((item.isFulfilled() ? "✅ " : "") + item.getName() + "   ·   " + priceLine);
        name.setFont(Theme.BODY_FONT);

        JLabel description = UiHelper.muted(item.getDescription() == null ? "" : item.getDescription());

        JProgressBar progress = new JProgressBar(0, 100);
        progress.setValue(item.getProgressPercent());
        progress.setStringPainted(true);
        progress.setForeground(item.isFulfilled() ? Theme.SUCCESS : Theme.PRIMARY);

        JPanel top = new JPanel(new GridLayout(2, 1));
        top.setOpaque(false);
        top.add(name);
        top.add(description);

        JButton editButton = UiHelper.smallButton("Edit", Theme.PRIMARY);
        JButton deleteButton = UiHelper.smallButton("Delete", Theme.ACCENT);

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AddEditItemDialog dialog = new AddEditItemDialog((Frame) SwingUtilities.getWindowAncestor(MyWishlistPanel.this), item);
                dialog.setVisible(true);
                if (dialog.wasSaved()) {
                    refresh();
                }
            }
        });
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteItem(item);
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        if (!item.isFulfilled()) {
            buttons.add(editButton);
        }
        buttons.add(deleteButton);

        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setOpaque(false);
        bottom.add(progress, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.EAST);

        row.add(top, BorderLayout.NORTH);
        row.add(bottom, BorderLayout.SOUTH);
        return row;
    }

    private void deleteItem(WishItem item) {
        int confirm = JOptionPane.showConfirmDialog(this, "Delete \"" + item.getName() + "\" from your wish list?",
            "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        Message request = new Message(Command.DELETE_WISH_ITEM);
        request.put("userId", Session.currentUser.getId());
        request.put("itemId", item.getId());
        try {
            Session.network.send(request);
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Network error: " + ex.getMessage());
        }
    }
}
