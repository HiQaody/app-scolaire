package com.monecole.gestion.views.dashboard;

import com.monecole.gestion.services.MoyenneService;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Map;

/**
 * Camembert dessiné en {@link Graphics2D}, sans dépendance
 * à une bibliothèque de graphiques externe.
 */
public class PieChartPanel extends JPanel {

    private static final int MARGIN = 30;
    private static final int PREFERRED_W = 360;
    private static final int PREFERRED_H = 300;

    private Map<MoyenneService.Mention, Integer> data;
    private String title;

    public PieChartPanel(String title) {
        setPreferredSize(new Dimension(PREFERRED_W, PREFERRED_H));
        this.title = title;
    }

    public void setData(Map<MoyenneService.Mention, Integer> data) {
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

        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.setColor(Color.DARK_GRAY);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, MARGIN, fm.getAscent() + MARGIN - 4);

        if (data == null || data.isEmpty()) {
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            g2.setColor(Color.GRAY);
            g2.drawString("Aucune donnée", w / 2 - 40, h / 2);
            return;
        }

        // filtrer et totaliser
        List<Map.Entry<MoyenneService.Mention, Integer>> entries =
            new java.util.ArrayList<>();
        int total = 0;
        for (var e : data.entrySet()) {
            if (e.getValue() > 0) {
                entries.add(e);
                total += e.getValue();
            }
        }
        if (total == 0) {
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            g2.setColor(Color.GRAY);
            g2.drawString("Aucune donnée", w / 2 - 40, h / 2);
            return;
        }

        int diameter = Math.min(w - 2 * MARGIN, h - 2 * MARGIN - 70);
        if (diameter <= 0) {
            return;
        }
        int cx = w / 2;
        int cy = h / 2 - 10;
        double start = Math.toRadians(-90);

        for (var e : entries) {
            double sweep = (double) e.getValue() / total * 2 * Math.PI;
            g2.setColor(e.getKey().getColor());
            g2.fillArc(cx - diameter / 2, cy - diameter / 2,
                diameter, diameter, (int) Math.toDegrees(start),
                (int) Math.toDegrees(sweep));
            start += sweep;
        }
        // trou central
        int hole = diameter / 3;
        g2.setColor(getBackground());
        g2.fillOval(cx - hole / 2, cy - hole / 2, hole, hole);
        g2.setColor(Color.DARK_GRAY);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        fm = g2.getFontMetrics();
        String txt = String.valueOf(total);
        g2.drawString(txt, cx - fm.stringWidth(txt) / 2,
            cy + fm.getAscent() / 2);

        // légende
        int legY = cy + diameter / 2 + 18;
        int legX = MARGIN;
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        for (var e : entries) {
            g2.setColor(e.getKey().getColor());
            g2.fillRect(legX, legY - 8, 14, 14);
            g2.setColor(Color.DARK_GRAY);
            fm = g2.getFontMetrics();
            String label = e.getKey().getLabel() + " (" + e.getValue() + ")";
            g2.drawString(label, legX + 20, legY + 4);
            legX += fm.stringWidth(label) + 24;
        }
    }
}
