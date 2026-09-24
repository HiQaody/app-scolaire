package com.monecole.gestion.utils;

import com.monecole.gestion.services.MoyenneService;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultKeyedValuesDataset;
import org.jfree.chart.ui.RectangleEdge;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Fabrique de graphiques JFreeChart pour le Dashboard.
 */
public final class ChartFactoryUtil {

    private ChartFactoryUtil() {}

    /**
     * Crée un graphique en camembert (pie chart) de la répartition des mentions.
     */
    public static ChartPanel createMentionPieChart(Map<MoyenneService.Mention, Integer> repartition, String title) {
        DefaultKeyedValuesDataset dataset = new DefaultKeyedValuesDataset();
        for (var entry : repartition.entrySet()) {
            if (entry.getValue() > 0) {
                dataset.setValue(entry.getKey().getLabel(), entry.getValue());
            }
        }

        JFreeChart chart = ChartFactory.createPieChart(
            title, dataset, true, true, false);

        formatChart(chart);
        return new ChartPanel(chart) {
            @Override
            public Dimension getPreferredSize() { return new Dimension(300, 250); }
        };
    }

    /**
     * Crée un histogramme des moyennes par matière.
     */
    public static ChartPanel createMatiereBarChart(Map<String, Double> moyennesParMatiere, String title) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (var entry : moyennesParMatiere.entrySet()) {
            double value = entry.getValue();
            if (value >= 0) {
                dataset.addValue(value, "Moyenne", entry.getKey());
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(
            title, "Matière", "Moyenne", dataset,
            PlotOrientation.VERTICAL, false, true, false);

        formatChart(chart);
        return new ChartPanel(chart) {
            @Override
            public Dimension getPreferredSize() { return new Dimension(400, 300); }
        };
    }

    private static void formatChart(JFreeChart chart) {
        chart.setBackgroundPaint(new Color(248, 249, 252));
        if (chart.getLegend() != null) {
            chart.getLegend().setPosition(RectangleEdge.BOTTOM);
            chart.getLegend().setHorizontalAlignment(HorizontalAlignment.CENTER);
        }
        chart.addSubtitle(new TextTitle(" "));
    }
}
