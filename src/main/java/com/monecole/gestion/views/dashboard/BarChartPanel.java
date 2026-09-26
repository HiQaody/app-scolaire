package com.monecole.gestion.views.dashboard;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Diagramme à barres dessiné en {@link Graphics2D}, sans dépendance
 * à une bibliothèque de graphiques externe.
 */
public class BarChartPanel extends JPanel {

    private static final int MARGIN_LEFT = 52;
    private static final int MARGIN_BOTTOM = 62;
    private static final int MARGIN_TOP = 20;
    private static final int MARGIN_RIGHT = 20;
    private static final int PREFERRED_W = 420;
    private static final int PREFERRED_H = 300;

    private Map<String, Double> data;
    private String title;

    public BarChartPanel(String title) {
        setPreferredSize(new Dimension(PREFERRED_W, PREFERRED_H));
        this.title = title;
    }

    public void setData(Map<String, Double> data) {
        this.data = data;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        int plotW = w - MARGIN_LEFT - MARGIN_RIGHT;
        int plotH = h - MARGIN_TOP - MARGIN_BOTTOM;

        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.setColor(Color.DARK_GRAY);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, MARGIN_LEFT, fm.getAscent() + MARGIN_TOP - 6);

        if (data == null || data.isEmpty() || plotW <= 0 || plotH <= 0) {
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            g2.setColor(Color.GRAY);
            g2.drawString("Aucune donnée", w / 2 - 40, h / 2);
            return;
        }

        // données triées par clé
        List<Map.Entry<String, Double>> entries = new ArrayList<>();
        double max = 0;
        for (var e : data.entrySet()) {
            double v = Math.max(e.getValue(), 0);
            if (v > max) {
                max = v;
            }
            entries.add(e);
        }
        if (max <= 0) {
            max = 20;
        }

        int n = entries.size();
        int slotW = plotW / n;
        int barW = (int) (slotW * 0.6);
        int gap = (slotW - barW) / 2;
        Color base = new Color(66, 135, 245);
        Color light = new Color(140, 175, 255);

        // graduations Y
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.setColor(Color.GRAY);
        int ticks = Math.min((int) max, 10);
        for (int i = 0; i <= ticks; i++) {
            double val = (double) max / ticks * i;
            int y = MARGIN_TOP + plotH - (int) (val / max * plotH);
            g2.drawString(String.format("%.1f", val), 8, y + 4);
            if (i > 0) {
                g2.setColor(new Color(0xE8, 0xE8, 0xEC));
                g2.drawLine(MARGIN_LEFT, y, MARGIN_LEFT + plotW, y);
                g2.setColor(Color.GRAY);
            }
        }
        g2.setColor(Color.DARK_GRAY);
        g2.drawLine(MARGIN_LEFT, MARGIN_TOP, MARGIN_LEFT, MARGIN_TOP + plotH);
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawLine(MARGIN_LEFT, MARGIN_TOP + plotH, MARGIN_LEFT + plotW, MARGIN_TOP + plotH);

        // barres
        for (int i = 0; i < n; i++) {
            double value = Math.max(entries.get(i).getValue(), 0);
            int barH = (int) (value / max * plotH);
            int x = MARGIN_LEFT + i * slotW + gap;
            int y = MARGIN_TOP + plotH - barH;

            if (barH > 0) {
                g2.setPaint(new java.awt.GradientPaint(x, y + barH, light, x, y, base));
                g2.fillRoundRect(x, y, barW, barH, 6, 6);
            }
            // étiquette valeur
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            String valTxt = String.format("%.1f", value);
            g2.drawString(valTxt, x + barW / 2 - valTxt.length() * 3, y - 6);

            // étiquette X (tournée)
            g2.setColor(Color.GRAY);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            String label = entries.get(i).getKey();
            int strW = g2.getFontMetrics().stringWidth(label);
            g2.rotate(-Math.toRadians(40), x + barW / 2.0, MARGIN_TOP + plotH + 12);
            g2.drawString(label, (int) (x + barW / 2.0 - strW / 2), MARGIN_TOP + plotH + 16);
            g2.rotate(Math.toRadians(40), x + barW / 2.0, MARGIN_TOP + plotH + 12);
        }
    }
}
