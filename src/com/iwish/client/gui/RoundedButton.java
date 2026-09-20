package com.iwish.client.gui;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class RoundedButton extends JButton {

    private Color background;
    private Color hoverBackground;
    private int radius;
    private boolean filled;

    public RoundedButton(String text, Color background, Color foreground, int radius, boolean filled) {
        super(text);
        this.background = background;
        this.hoverBackground = background.darker();
        this.radius = radius;
        this.filled = filled;
        setForeground(foreground);
        setFont(Theme.BUTTON_FONT);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 24, 12, 24));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (filled) {
            g2.setColor(getModel().isRollover() ? hoverBackground : background);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        } else {
            g2.setColor(background);
            g2.setStroke(new java.awt.BasicStroke(1.5f));
            g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, radius, radius);
        }
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(Math.max(size.width, 120), Math.max(size.height, 44));
    }
}