package com.iwish.client.gui;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;

    public class GradientPanel extends JPanel {

        private JComponent shadowTarget;
        private final Color topColor;
        private final Color bottomColor;

        public GradientPanel() {
            this(Theme.BACKGROUND, Theme.BACKGROUND);
        }

        public GradientPanel(Color topColor, Color bottomColor) {
            this.topColor = topColor;
            this.bottomColor = bottomColor;
            setOpaque(true);
        }

        public void setShadowTarget(JComponent component) {
            this.shadowTarget = component;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            LinearGradientPaint gradient = new LinearGradientPaint(
                    0, 0, getWidth(), getHeight(),
                    new float[]{0f, 1f},
                    new Color[]{topColor, bottomColor}
            );
            g2.setPaint(gradient);
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (shadowTarget != null && shadowTarget.isShowing()) {
                paintSoftShadow(g2, shadowTarget.getBounds());
            }
            g2.dispose();
        }

        private void paintSoftShadow(Graphics2D g2, Rectangle bounds) {
            int radius = 30;
            int layers = 22;
            int yOffset = 10;
            for (int i = layers; i > 0; i--) {
                int alpha = Math.max(1, 10 - (i * 10 / layers));
                g2.setColor(new Color(45, 52, 54, alpha));
                int spread = i;
                g2.fillRoundRect(
                        bounds.x - spread, bounds.y - spread + yOffset,
                        bounds.width + spread * 2, bounds.height + spread * 2,
                        radius + spread, radius + spread
                );
            }
        }
    }

