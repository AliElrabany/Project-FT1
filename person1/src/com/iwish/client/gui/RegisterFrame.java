package com.iwish.client.gui;

import com.iwish.client.NetworkClient;
import com.iwish.shared.Command;
import com.iwish.shared.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RegisterFrame extends JFrame {

    private JTextField fullNameField;
    private JTextField usernameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JLabel statusLabel;
    private String host;
    private String portText;

    public RegisterFrame(String host, String portText) {
        super("i-Wish - Create Account");
        this.host = host;
        this.portText = portText;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(520, 740);
        setLocationRelativeTo(null);
        setResizable(false);
        setIconImage(IconFactory.createIcon());

        GradientPanel background = new GradientPanel(Theme.GRADIENT_TOP, Theme.GRADIENT_BOTTOM);
        background.setLayout(new GridBagLayout());
        JPanel card = buildCard();
        background.setShadowTarget(card);
        background.add(card, new GridBagConstraints());
        setContentPane(background);
    }

    private JPanel buildCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(28, Theme.BORDER, 1),
                BorderFactory.createEmptyBorder(44, 40, 40, 40)
        ));
        card.setPreferredSize(new Dimension(420, 620));
        card.setMaximumSize(new Dimension(420, 620));

        JLabel title = new JLabel("Join i-Wish \uD83C\uDF89");
        title.setFont(new Font("SansSerif", Font.BOLD, 30));
        title.setForeground(Theme.PRIMARY_DARK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Create an account to get started");
        subtitle.setFont(Theme.BODY_FONT);
        subtitle.setForeground(Theme.MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        fullNameField = UiHelper.field();
        usernameField = UiHelper.field();
        emailField = UiHelper.field();
        passwordField = UiHelper.passwordField();

        JComponent[] fields = { fullNameField, usernameField, emailField, passwordField };
        for (JComponent f : fields) {
            f.setMaximumSize(new Dimension(340, 46));
            f.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        JButton createButton = UiHelper.primaryButton("Create Account");
        createButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        createButton.setMaximumSize(new Dimension(340, 48));
        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                attemptRegister();
            }
        });

        statusLabel = new JLabel(" ");
        statusLabel.setFont(Theme.SMALL_FONT);
        statusLabel.setForeground(Theme.ACCENT);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(30));
        card.add(fieldLabel("Full name"));
        card.add(Box.createVerticalStrut(6));
        card.add(fullNameField);
        card.add(Box.createVerticalStrut(16));
        card.add(fieldLabel("Username"));
        card.add(Box.createVerticalStrut(6));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(16));
        card.add(fieldLabel("Email (optional)"));
        card.add(Box.createVerticalStrut(6));
        card.add(emailField);
        card.add(Box.createVerticalStrut(16));
        card.add(fieldLabel("Password"));
        card.add(Box.createVerticalStrut(6));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(26));
        card.add(createButton);
        card.add(Box.createVerticalStrut(10));
        card.add(statusLabel);

        getRootPane().setDefaultButton(createButton);
        return card;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.SMALL_FONT);
        label.setForeground(Theme.MUTED);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    private void attemptRegister() {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (fullName.length() == 0 || username.length() == 0 || password.length() == 0) {
            statusLabel.setText("Full name, username and password are required.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText.trim());
        } catch (NumberFormatException e) {
            statusLabel.setText("Port must be a number.");
            return;
        }

        Message request = new Message(Command.REGISTER);
        request.put("username", username);
        request.put("password", password);
        request.put("fullName", fullName);
        request.put("email", email);

        try {
            NetworkClient client = new NetworkClient(host, port);
            Message response = client.send(request);
            if (response.success) {
                JOptionPane.showMessageDialog(this, response.text, "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                statusLabel.setText(response.text);
            }
        } catch (Exception e) {
            statusLabel.setText("Could not reach server: " + e.getMessage());
        }
    }
}