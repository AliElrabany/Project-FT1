package com.iwish.client.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

public class UiHelper {

    public static JButton primaryButton(String text) {
        return new RoundedButton(text, Theme.PRIMARY, Color.WHITE, 22, true);
    }

    public static JButton secondaryButton(String text) {
        return new RoundedButton(text, Theme.PRIMARY, Theme.PRIMARY, 22, false);
    }

    public static JButton smallButton(String text, Color background) {
        RoundedButton button = new RoundedButton(text, background, Color.WHITE, 16, true);
        button.setFont(Theme.SMALL_FONT);
        button.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return button;
    }

    public static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.HEADING_FONT);
        label.setForeground(Theme.TEXT);
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.SMALL_FONT);
        label.setForeground(Theme.MUTED);
        return label;
    }

    public static JTextField field() {
        JTextField field = new JTextField();
        field.setFont(Theme.BODY_FONT);
        field.setBorder(new RoundedBorder(14, Theme.BORDER, 1));
        field.setBackground(new Color(0xFAF9FF));
        return field;
    }

    public static JPasswordField passwordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(Theme.BODY_FONT);
        field.setBorder(new RoundedBorder(14, Theme.BORDER, 1));
        field.setBackground(new Color(0xFAF9FF));
        return field;
    }
}