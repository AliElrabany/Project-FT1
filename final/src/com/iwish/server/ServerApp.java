package com.iwish.server;

import com.iwish.client.gui.GradientPanel;
import com.iwish.client.gui.IconFactory;
import com.iwish.client.gui.RoundedBorder;
import com.iwish.client.gui.RoundedButton;
import com.iwish.client.gui.RoundedPanel;
import com.iwish.client.gui.Theme;
import com.iwish.client.gui.UiHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A small admin GUI around Server, so starting and stopping the i-Wish
 * server is a couple of clicks instead of a terminal window you have to
 * keep open and Ctrl+C.
 */
public class ServerApp extends JFrame {

    private final Server server = new Server();
    private Thread serverThread;

    private JTextField portField;
    private JTextField dbNameField;
    private JButton startButton;
    private JButton stopButton;
    private JLabel statusDot;
    private JLabel statusText;
    private JTextArea console;

    public ServerApp() {
        super("i-Wish Server");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(760, 620);
        setMinimumSize(new Dimension(620, 460));
        setLocationRelativeTo(null);
        setIconImage(IconFactory.createIcon());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        setContentPane(root);

        redirectSystemStreams();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (server.isRunning()) {
                    int choice = JOptionPane.showConfirmDialog(
                            ServerApp.this,
                            "The server is still running. Stop it and exit?",
                            "Stop server?",
                            JOptionPane.YES_NO_OPTION
                    );
                    if (choice != JOptionPane.YES_OPTION) {
                        return;
                    }
                    stopServer();
                }
                dispose();
                System.exit(0);
            }
        });

        setStoppedState();
    }

    private JPanel buildHeader() {
        GradientPanel header = new GradientPanel(Theme.HEADER_GRADIENT_START, Theme.HEADER_GRADIENT_END);
        header.setLayout(new BorderLayout());
        header.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel iconLabel = new JLabel(new ImageIcon(IconFactory.createIcon(34)));
        JLabel title = new JLabel("i-Wish Server");
        title.setFont(Theme.TITLE_FONT);
        title.setForeground(Color.WHITE);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleRow.setOpaque(false);
        titleRow.add(iconLabel);
        titleRow.add(title);

        statusDot = new JLabel("\u25CF");
        statusDot.setFont(new Font("SansSerif", Font.PLAIN, 18));
        statusText = new JLabel("Stopped");
        statusText.setFont(Theme.BODY_FONT);
        statusText.setForeground(Color.WHITE);

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        statusRow.setOpaque(false);
        statusRow.add(statusDot);
        statusRow.add(statusText);

        header.add(titleRow, BorderLayout.WEST);
        header.add(statusRow, BorderLayout.EAST);
        return header;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(20, 24, 20, 24));

        body.add(buildControlsCard(), BorderLayout.NORTH);
        body.add(buildConsole(), BorderLayout.CENTER);
        return body;
    }

    private JPanel buildControlsCard() {
        JPanel card = new RoundedPanel(20);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(20, Theme.BORDER, 1),
                new EmptyBorder(20, 24, 20, 24)
        ));

        JLabel heading = UiHelper.heading("Server settings");

        portField = UiHelper.field();
        portField.setText(String.valueOf(Server.DEFAULT_PORT));
        portField.setColumns(6);
        portField.setMaximumSize(new Dimension(90, 42));

        dbNameField = UiHelper.field();
        dbNameField.setText("iwish");
        dbNameField.setColumns(12);
        dbNameField.setMaximumSize(new Dimension(160, 42));

        JPanel fieldsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        fieldsRow.setOpaque(false);
        fieldsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldsRow.add(labeledField("Port", portField));
        fieldsRow.add(labeledField("Database name", dbNameField));

        startButton = UiHelper.primaryButton("\u25B6  Start Server");
        startButton.addActionListener(this::onStart);

        stopButton = new RoundedButton("\u25A0  Stop Server", Theme.ACCENT, Color.WHITE, 22, true);
        stopButton.addActionListener(this::onStop);

        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        buttonsRow.setOpaque(false);
        buttonsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonsRow.add(startButton);
        buttonsRow.add(stopButton);

        card.add(heading);
        card.add(Box.createVerticalStrut(14));
        card.add(fieldsRow);
        card.add(Box.createVerticalStrut(16));
        card.add(buttonsRow);
        return card;
    }

    private JPanel labeledField(String label, JComponent field) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel l = UiHelper.muted(label);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(l);
        panel.add(Box.createVerticalStrut(4));
        panel.add(field);
        return panel;
    }

    private JScrollPane buildConsole() {
        console = new JTextArea();
        console.setEditable(false);
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        console.setBackground(new Color(0x1E1B2E));
        console.setForeground(new Color(0xD6D2F0));
        console.setCaretColor(Color.WHITE);
        console.setBorder(new EmptyBorder(14, 16, 14, 16));
        appendLine("Ready. Set the port and database name above, then press Start Server.");

        JScrollPane scroll = new JScrollPane(console);
        scroll.setBorder(new RoundedBorder(20, Theme.BORDER, 1));
        return scroll;
    }

    private void onStart(ActionEvent e) {
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Port must be a number.", "Invalid port", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String dbName = dbNameField.getText().trim();
        if (dbName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Database name can't be empty.", "Invalid database", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setStartingState();

        serverThread = new Thread(() -> {
            try {
                server.start(port, dbName);
            } catch (Exception ex) {
                appendLineAsync("[Server] Failed to start: " + ex.getMessage());
            } finally {
                SwingUtilities.invokeLater(this::setStoppedState);
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        Timer check = new Timer(400, ev -> {
            if (server.isRunning()) {
                setRunningState();
            }
        });
        check.setRepeats(false);
        check.start();
    }

    private void onStop(ActionEvent e) {
        stopServer();
    }

    private void stopServer() {
        setStoppingState();
        new Thread(() -> {
            server.stop();
            SwingUtilities.invokeLater(this::setStoppedState);
        }).start();
    }

    private void setStoppedState() {
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        portField.setEnabled(true);
        dbNameField.setEnabled(true);
        statusDot.setForeground(new Color(0xFF7675));
        statusText.setText("Stopped");
    }

    private void setStartingState() {
        startButton.setEnabled(false);
        stopButton.setEnabled(false);
        portField.setEnabled(false);
        dbNameField.setEnabled(false);
        statusDot.setForeground(new Color(0xFDCB6E));
        statusText.setText("Starting...");
    }

    private void setRunningState() {
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        statusDot.setForeground(Theme.SUCCESS);
        statusText.setText("Running on port " + portField.getText().trim());
    }

    private void setStoppingState() {
        stopButton.setEnabled(false);
        statusDot.setForeground(new Color(0xFDCB6E));
        statusText.setText("Stopping...");
    }

    private void appendLine(String line) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        console.append("[" + time + "] " + line + "\n");
        console.setCaretPosition(console.getDocument().getLength());
    }

    private void appendLineAsync(String line) {
        SwingUtilities.invokeLater(() -> appendLine(line));
    }

    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public void write(int b) {
                char c = (char) b;
                if (c == '\n') {
                    String line = buffer.toString();
                    buffer.setLength(0);
                    appendLineAsync(line);
                } else if (c != '\r') {
                    buffer.append(c);
                }
            }
        };
        PrintStream printStream = new PrintStream(out, true);
        System.setOut(printStream);
        System.setErr(printStream);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerApp().setVisible(true));
    }
}
