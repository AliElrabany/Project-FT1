package com.iwish.client.gui;

import com.iwish.client.NetworkClient;
import com.iwish.client.Session;
import com.iwish.shared.Command;
import com.iwish.shared.Message;
import com.iwish.shared.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginFrame extends JFrame {

    private JTextField hostField;
    private JTextField portField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel statusLabel;

    public LoginFrame() {
        super("i-Wish - Sign In");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 780);
        setLocationRelativeTo(null);
        setResizable(false);
        setIconImage(IconFactory.createIcon());

        GradientPanel background = new GradientPanel(Theme.GRADIENT_TOP, Theme.GRADIENT_BOTTOM);
        background.setLayout(new GridBagLayout());

        JPanel card = buildCard();
        background.setShadowTarget(card);

        GridBagConstraints gbc = new GridBagConstraints();
        background.add(card, gbc);

        setContentPane(background);
    }

    private JPanel buildCard() {
        JPanel card = new RoundedPanel(28);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(28, Theme.BORDER, 1),
                BorderFactory.createEmptyBorder(44, 40, 40, 40)
        ));
        card.setPreferredSize(new Dimension(420, 660));
        card.setMaximumSize(new Dimension(420, 660));

        JLabel logo = new JLabel("i-Wish");
        logo.setFont(new Font("SansSerif", Font.BOLD, 40));
        logo.setForeground(Theme.PRIMARY_DARK);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel gift = new JLabel(new ImageIcon(IconFactory.createIcon(40)));
        gift.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        logoRow.setOpaque(false);
        logoRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoRow.add(logo);
        logoRow.add(gift);

        JLabel subtitle = new JLabel("Make your friends' wishes come true");
        subtitle.setFont(Theme.BODY_FONT);
        subtitle.setForeground(Theme.MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        hostField = UiHelper.field();
        hostField.setText("localhost");
        portField = UiHelper.field();
        portField.setText("5050");
        usernameField = UiHelper.field();
        passwordField = UiHelper.passwordField();

        JPanel serverRow = new JPanel(new GridLayout(1, 2, 12, 0));
        serverRow.setOpaque(false);
        serverRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        serverRow.setMaximumSize(new Dimension(340, 46));
        serverRow.add(hostField);
        serverRow.add(portField);

        usernameField.setMaximumSize(new Dimension(340, 46));
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordField.setMaximumSize(new Dimension(340, 46));
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton loginButton = UiHelper.primaryButton("Sign In");
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(340, 48));
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                attemptLogin();
            }
        });

        statusLabel = new JLabel(" ");
        statusLabel.setFont(Theme.SMALL_FONT);
        statusLabel.setForeground(Theme.ACCENT);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JSeparator divider = new JSeparator();
        divider.setMaximumSize(new Dimension(340, 1));
        divider.setForeground(Theme.BORDER);
        divider.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton registerButton = UiHelper.secondaryButton("Create New Account");
        registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerButton.setMaximumSize(new Dimension(340, 48));
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openRegister();
            }
        });

        card.add(logoRow);
        card.add(Box.createVerticalStrut(6));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(36));
        card.add(fieldLabel("Server"));
        card.add(Box.createVerticalStrut(6));
        card.add(serverRow);
        card.add(Box.createVerticalStrut(20));
        card.add(fieldLabel("Username"));
        card.add(Box.createVerticalStrut(6));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(16));
        card.add(fieldLabel("Password"));
        card.add(Box.createVerticalStrut(6));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(24));
        card.add(loginButton);
        card.add(Box.createVerticalStrut(10));
        card.add(statusLabel);
        card.add(Box.createVerticalGlue());
        card.add(divider);
        card.add(Box.createVerticalStrut(20));
        card.add(registerButton);

        getRootPane().setDefaultButton(loginButton);
        return card;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.SMALL_FONT);
        label.setForeground(Theme.MUTED);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    private void attemptLogin() {
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            statusLabel.setText("Port must be a number.");
            return;
        }
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.length() == 0 || password.length() == 0) {
            statusLabel.setText("Please enter a username and password.");
            return;
        }

        statusLabel.setForeground(Theme.MUTED);
        statusLabel.setText("Connecting...");

        NetworkClient client = new NetworkClient(host, port);
        Message request = new Message(Command.LOGIN);
        request.put("username", username);
        request.put("password", password);

        try {
            Message response = client.send(request);
            if (response.success) {
                Session.network = client;
                Session.currentUser = (User) response.get("user");
                dispose();
                new MainFrame().setVisible(true);
            } else {
                statusLabel.setForeground(Theme.ACCENT);
                statusLabel.setText(response.text);
            }
        } catch (Exception e) {
            statusLabel.setForeground(Theme.ACCENT);
            statusLabel.setText("Could not reach server: " + e.getMessage());
        }
    }

    private void openRegister() {
        String host = hostField.getText().trim();
        String portText = portField.getText().trim();
        RegisterFrame frame = new RegisterFrame(host, portText);
        frame.setVisible(true);
    }
}