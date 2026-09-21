package com.iwish.client.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class IconFactory {

    public static BufferedImage createIcon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int arc = size / 4;
        g.setColor(Theme.PRIMARY);
        g.fillRoundRect(0, 0, size, size, arc, arc);

        g.setColor(Color.WHITE);
        int boxMargin = (int) (size * 0.22);
        int boxTop = (int) (size * 0.42);
        int boxWidth = size - boxMargin * 2;
        int boxHeight = size - boxTop - (int) (size * 0.12);
        g.fillRect(boxMargin, boxTop, boxWidth, boxHeight);

        g.setColor(Theme.ACCENT);
        int ribbonWidth = (int) (size * 0.14);
        g.fillRect(size / 2 - ribbonWidth / 2, boxTop, ribbonWidth, boxHeight);

        g.setColor(Color.WHITE);
        int lidHeight = (int) (size * 0.10);
        int lidMargin = (int) (size * 0.16);
        g.fillRect(lidMargin, boxTop - lidHeight, size - lidMargin * 2, lidHeight);
        g.setColor(Theme.ACCENT);
        g.fillRect(size / 2 - ribbonWidth / 2, boxTop - lidHeight, ribbonWidth, lidHeight);

        g.setColor(Theme.ACCENT);
        int bowSize = (int) (size * 0.13);
        g.fillOval(size / 2 - bowSize, boxTop - lidHeight - bowSize / 2, bowSize, bowSize);
        g.fillOval(size / 2, boxTop - lidHeight - bowSize / 2, bowSize, bowSize);

        g.dispose();
        return image;
    }

    public static BufferedImage createIcon() {
        return createIcon(128);
    }
}
