package com.iwish.client.gui;

import com.iwish.client.Session;
import com.iwish.shared.Command;
import com.iwish.shared.Message;
import com.iwish.shared.WishItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ContributeDialog extends JDialog {

    private JTextField amountField;
    private JLabel statusLabel;
    private boolean contributed = false;

    public ContributeDialog(Frame owner, final WishItem item) {
        super(owner, "Contribute to \"" + item.getName() + "\"", true);
        setSize(400, 290);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel itemName = UiHelper.heading(item.getName());
        JLabel priceInfo = UiHelper.muted("Price: $" + item.getPrice() + "   ·   Still needed: $" + item.getAmountRemaining());

        amountField = UiHelper.field();
        amountField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        amountField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton contributeButton = UiHelper.primaryButton("Contribute");
        contributeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        contributeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submit(item);
            }
        });

        statusLabel = new JLabel(" ");
        statusLabel.setFont(Theme.SMALL_FONT);
        statusLabel.setForeground(Theme.ACCENT);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        root.add(itemName);
        root.add(Box.createVerticalStrut(4));
        root.add(priceInfo);
        root.add(Box.createVerticalStrut(16));
        root.add(UiHelper.muted("How much would you like to contribute?"));
        root.add(amountField);
        root.add(Box.createVerticalStrut(14));
        root.add(contributeButton);
        root.add(Box.createVerticalStrut(8));
        root.add(statusLabel);

        setContentPane(root);
        getRootPane().setDefaultButton(contributeButton);
    }

    private void submit(WishItem item) {
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim());
        } catch (NumberFormatException e) {
            statusLabel.setText("Enter a valid amount.");
            return;
        }
        if (amount <= 0) {
            statusLabel.setText("Amount must be greater than zero.");
            return;
        }

        Message request = new Message(Command.CONTRIBUTE);
        request.put("userId", Session.currentUser.getId());
        request.put("itemId", item.getId());
        request.put("amount", amount);

        try {
            Message response = Session.network.send(request);
            if (response.success) {
                contributed = true;
                JOptionPane.showMessageDialog(this, response.text, "Thank you!", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                statusLabel.setText(response.text);
            }
        } catch (Exception ex) {
            statusLabel.setText("Network error: " + ex.getMessage());
        }
    }

    public boolean didContribute() {
        return contributed;
    }
}