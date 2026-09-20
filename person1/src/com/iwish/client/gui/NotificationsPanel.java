package com.iwish.client.gui;

import com.iwish.client.Session;
import com.iwish.shared.Command;
import com.iwish.shared.Message;
import com.iwish.shared.Notification;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * The client polls GET_NOTIFICATIONS every few seconds with a Swing Timer.
 * This is a simple, reliable way to satisfy requirements 8 & 9 (buyer and
 * receiver notifications) without needing a second always-open socket per client.
 */
public class NotificationsPanel extends JPanel {

    private JPanel notificationsList;
    private Timer pollTimer;

    public NotificationsPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        top.add(UiHelper.heading("Notifications"), BorderLayout.WEST);

        JButton markReadButton = UiHelper.secondaryButton("Mark All as Read");
        markReadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                markAllRead();
            }
        });
        top.add(markReadButton, BorderLayout.EAST);

        notificationsList = new JPanel();
        notificationsList.setLayout(new BoxLayout(notificationsList, BoxLayout.Y_AXIS));
        notificationsList.setOpaque(false);

        JScrollPane scroll = new JScrollPane(notificationsList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(14);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        refresh();

        pollTimer = new Timer(5000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
        pollTimer.start();
    }

    public void refresh() {
        try {
            Message request = new Message(Command.GET_NOTIFICATIONS);
            request.put("userId", Session.currentUser.getId());
            Message response = Session.network.send(request);

            @SuppressWarnings("unchecked")
            List<Notification> notifications = (List<Notification>) response.get("notifications");

            notificationsList.removeAll();
            if (notifications == null || notifications.isEmpty()) {
                notificationsList.add(UiHelper.muted("Nothing here yet - notifications about your friends and gifts will show up here."));
            } else {
                for (int i = 0; i < notifications.size(); i++) {
                    notificationsList.add(buildRow(notifications.get(i)));
                    notificationsList.add(Box.createVerticalStrut(8));
                }
            }
            notificationsList.revalidate();
            notificationsList.repaint();
        } catch (Exception ex) {
            // Silently skip a failed poll; the user can still use the rest of the app.
        }
    }

    private JPanel buildRow(Notification notification) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(notification.isRead() ? Theme.CARD : new Color(0xEDE9FF));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER, 1, true),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel message = new JLabel((notification.isRead() ? "" : "🔔 ") + notification.getMessage());
        message.setFont(Theme.BODY_FONT);

        JLabel time = UiHelper.muted(notification.getCreatedAt());

        row.add(message, BorderLayout.CENTER);
        row.add(time, BorderLayout.EAST);
        return row;
    }

    private void markAllRead() {
        Message request = new Message(Command.MARK_NOTIFICATIONS_READ);
        request.put("userId", Session.currentUser.getId());
        try {
            Session.network.send(request);
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Network error: " + ex.getMessage());
        }
    }
}
