package com.monecole.gestion.views.grades;

import com.monecole.gestion.services.NoteService;
import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.List;

/**
 * Panneau de statistiques d'une évaluation, recalculé à chaque modification
 * de la saisie.
 * <p>
 * Les valeurs sont toutes ramenées sur 20 pour rester comparables d'une
 * évaluation à l'autre, quel que soit le barème utilisé. L'histogramme est
 * dessiné entièrement en {@link Graphics2D}, sans dépendance à une
 * bibliothèque de graphiques externe.
 */
public class StatsPanel extends JPanel {

    private final JLabel notesLabel = valueLabel();
    private final JLabel moyenneLabel = valueLabel();
    private final JLabel medianeLabel = valueLabel();
    private final JLabel minLabel = valueLabel();
    private final JLabel maxLabel = valueLabel();
    private final JLabel ecartTypeLabel = valueLabel();
    private final JLabel reussiteLabel = valueLabel();
    private final JLabel absentsLabel = valueLabel();

    private final HistogramPanel histogramPanel = new HistogramPanel();

    public StatsPanel() {
        super(new BorderLayout(10, 5));
        setBorder(BorderFactory.createTitledBorder("Statistiques de l'évaluation"));

        JPanel tuiles = new JPanel(new GridLayout(2, 4, 8, 4));
        tuiles.setOpaque(false);
        tuiles.add(tuile("Notes saisies", notesLabel));
        tuiles.add(tuile("Moyenne /20", moyenneLabel));
        tuiles.add(tuile("Médiane /20", medianeLabel));
        tuiles.add(tuile("Taux de réussite", reussiteLabel));
        tuiles.add(tuile("Note minimale", minLabel));
        tuiles.add(tuile("Note maximale", maxLabel));
        tuiles.add(tuile("Écart-type", ecartTypeLabel));
        tuiles.add(tuile("Non notés", absentsLabel));

        add(tuiles, BorderLayout.NORTH);
        add(histogramPanel, BorderLayout.CENTER);
        setStatistiques(null);
    }

    /** Met à jour l'affichage ; {@code null} vide le panneau. */
    public void setStatistiques(NoteService.StatsEvaluation stats) {
        if (stats == null) {
            notesLabel.setText("—");
            moyenneLabel.setText("—");
            medianeLabel.setText("—");
            minLabel.setText("—");
            maxLabel.setText("—");
            ecartTypeLabel.setText("—");
            reussiteLabel.setText("—");
            absentsLabel.setText("—");
            histogramPanel.setData(null, null);
            return;
        }

        notesLabel.setText(stats.notesSaisies() + " / " + stats.effectif());
        moyenneLabel.setText(stats.moyenneFormatee());
        medianeLabel.setText(stats.mediane() == null ? "—" : String.format("%.2f", stats.mediane()));
        minLabel.setText(stats.minimum() == null ? "—" : String.format("%.2f", stats.minimum()));
        maxLabel.setText(stats.maximum() == null ? "—" : String.format("%.2f", stats.maximum()));
        ecartTypeLabel.setText(stats.ecartType() == null ? "—" : String.format("%.2f", stats.ecartType()));
        reussiteLabel.setText(stats.tauxReussitePourcentage());
        absentsLabel.setText(String.valueOf(stats.nbSansNote()));

        if (stats.moyenne() != null) {
            moyenneLabel.setForeground(stats.moyenne() >= NoteService.SEUIL_REUSSITE
                ? new Color(0x1E, 0x88, 0x3E) : new Color(0xC0, 0x39, 0x2B));
        }

        histogramPanel.setData(stats.histogramme(), "Répartition des notes");
    }

    private static JPanel tuile(String titre, JLabel valeur) {
        JPanel panneau = new JPanel(new BorderLayout(0, 2));
        panneau.setOpaque(false);
        JLabel t = new JLabel(titre);
        t.setFont(t.getFont().deriveFont(Font.PLAIN, 11f));
        t.setForeground(Color.GRAY);
        panneau.add(t, BorderLayout.NORTH);
        panneau.add(valeur, BorderLayout.CENTER);
        return panneau;
    }

    private static JLabel valueLabel() {
        JLabel label = new JLabel("—");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
        return label;
    }

    /** Histogramme dessiné entièrement en {@link Graphics2D}. */
    private static class HistogramPanel extends JPanel {

        private List<Integer> tranches;
        private String title;

        private static final int MARGIN_LEFT = 48;
        private static final int MARGIN_BOTTOM = 52;
        private static final int MARGIN_TOP = 16;
        private static final int MARGIN_RIGHT = 16;

        HistogramPanel() {
            setPreferredSize(new java.awt.Dimension(320, 200));
        }

        void setData(List<Integer> tranches, String title) {
            this.tranches = tranches;
            this.title = title;
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

            if (tranches == null || tranches.isEmpty() || plotW <= 0 || plotH <= 0) {
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                g2.setColor(Color.GRAY);
                g2.drawString("Aucune donnée", w / 2 - 40, h / 2);
                return;
            }

            int max = 1;
            for (int v : tranches) {
                if (v > max) {
                    max = v;
                }
            }

            // Titre
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.setColor(Color.DARK_GRAY);
            g2.drawString(title, MARGIN_LEFT, MARGIN_TOP - 2);

            // Axe Y : graduations
            int ticks = Math.min(max, 10);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(Color.GRAY);
            for (int i = 0; i <= ticks; i++) {
                int val = (int) Math.round((double) max / ticks * i);
                int y = MARGIN_TOP + plotH - val * plotH / max;
                g2.drawString(String.valueOf(val), MARGIN_LEFT - 26, y + 4);
                if (i > 0) {
                    g2.setColor(new Color(0xE8, 0xE8, 0xEC));
                    g2.drawLine(MARGIN_LEFT, y, MARGIN_LEFT + plotW, y);
                    g2.setColor(Color.GRAY);
                }
            }

            // Barres
            int n = tranches.size();
            int slotW = plotW / n;
            int barW = (int) (slotW * 0.72);
            int gap = (slotW - barW) / 2;
            Color base = new Color(66, 135, 245);
            Color light = new Color(140, 175, 255);

            for (int i = 0; i < n; i++) {
                int count = tranches.get(i);
                int barH = (int) ((double) count / max * plotH);
                int x = MARGIN_LEFT + i * slotW + gap;
                int y = MARGIN_TOP + plotH - barH;

                if (barH > 0) {
                    g2.setPaint(new GradientPaint(x, y + barH, light, x, y, base));
                    g2.fillRoundRect(x, y, barW, barH, 6, 6);
                }
                // valeur au-dessus
                g2.setColor(Color.DARK_GRAY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                String txt = String.valueOf(count);
                g2.drawString(txt, x + barW / 2 - txt.length() * 3, y - 6);

                // étiquette X
                g2.setColor(Color.GRAY);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                int bas = i * 2;
                String label = bas + "-" + (bas + 2);
                int strW = g2.getFontMetrics().stringWidth(label);
                g2.rotate(-Math.toRadians(45), x + barW / 2.0, MARGIN_TOP + plotH + 10);
                g2.drawString(label, (int) (x + barW / 2.0 - strW / 2), MARGIN_TOP + plotH + 14);
                g2.rotate(Math.toRadians(45), x + barW / 2.0, MARGIN_TOP + plotH + 10);
            }

            // axe Y
            g2.setColor(Color.DARK_GRAY);
            g2.drawLine(MARGIN_LEFT, MARGIN_TOP, MARGIN_LEFT, MARGIN_TOP + plotH);
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawLine(MARGIN_LEFT, MARGIN_TOP + plotH, MARGIN_LEFT + plotW, MARGIN_TOP + plotH);
        }
    }
}
