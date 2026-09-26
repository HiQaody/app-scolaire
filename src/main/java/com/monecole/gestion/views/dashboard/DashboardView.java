package com.monecole.gestion.views.dashboard;

import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.models.AnneeScolaire;
import com.monecole.gestion.models.Classe;
import com.monecole.gestion.services.MoyenneService;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Vue du tableau de bord (Dashboard).
 * Affiche des statistiques et des graphiques dessines entierement en Swing.
 */
public class DashboardView extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(DashboardView.class.getName());

    private final MoyenneService moyenneService = new MoyenneService();
    private final JPanel statsContainer;
    private final JPanel chartsContainer;
    private JLabel tauxReussiteLabel;

    public DashboardView() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);

        statsContainer = new JPanel(new GridLayout(2, 4, 10, 10));
        statsContainer.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        add(statsContainer, BorderLayout.CENTER);

        chartsContainer = new JPanel(new GridLayout(1, 2, 10, 10));
        chartsContainer.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        add(chartsContainer, BorderLayout.SOUTH);

        loadDashboardData();
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel title = new JLabel("Tableau de bord");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        panel.add(title, BorderLayout.WEST);

        tauxReussiteLabel = new JLabel("Taux de réussite : —");
        tauxReussiteLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panel.add(tauxReussiteLabel, BorderLayout.EAST);

        return panel;
    }

    private void loadDashboardData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private int etudiants, classes, matieres, enseignants, enseignements, evaluations;
            private AnneeScolaire activeYear;
            private Classe firstClasse;

            @Override
            protected Void doInBackground() throws Exception {
                DaoFactory df = DaoFactory.getInstance();
                etudiants = (int) df.getEtudiantDao().count();
                classes = (int) df.getClasseDao().count();
                matieres = (int) df.getMatiereDao().count();
                enseignants = (int) df.getEnseignantDao().count();
                enseignements = (int) df.getEnseignementDao().count();
                evaluations = (int) df.getEvaluationDao().count();
                activeYear = df.getAnneeScolaireDao().findActive();

                List<Classe> allClasses = df.getClasseDao().findAll();
                if (!allClasses.isEmpty()) {
                    firstClasse = allClasses.get(0);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    buildStatCards(etudiants, classes, matieres, enseignants,
                        enseignements, evaluations, activeYear);
                    buildCharts(firstClasse);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur chargement dashboard", e);
                }
            }
        };
        worker.execute();
    }

    private void buildStatCards(int etudiants, int classes, int matieres,
            int enseignants, int enseignements, int evaluations,
            AnneeScolaire activeYear) {
        statsContainer.removeAll();
        statsContainer.add(buildStatCard("Étudiants", String.valueOf(etudiants)));
        statsContainer.add(buildStatCard("Classes", String.valueOf(classes)));
        statsContainer.add(buildStatCard("Matières", String.valueOf(matieres)));
        statsContainer.add(buildStatCard("Enseignants", String.valueOf(enseignants)));

        String yearLabel = activeYear != null ? activeYear.libelle() : "—";
        statsContainer.add(buildStatCard("Année active", yearLabel));
        try {
            int users = (int) DaoFactory.getInstance().getUtilisateurDao().count();
            statsContainer.add(buildStatCard("Utilisateurs", String.valueOf(users)));
        } catch (Exception e) {
            statsContainer.add(buildStatCard("Utilisateurs", "—"));
        }
        statsContainer.add(buildStatCard("Enseignements", String.valueOf(enseignements)));
        statsContainer.add(buildStatCard("Évaluations", String.valueOf(evaluations)));

        statsContainer.revalidate();
        statsContainer.repaint();
    }

    private void buildCharts(Classe classe) {
        chartsContainer.removeAll();

        if (classe == null) {
            chartsContainer.add(buildPlaceholder("Aucune classe disponible"));
        } else {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                private java.util.Map<MoyenneService.Mention, Integer> repartition;
                private java.util.Map<String, Double> moyennesParMatiere;

                @Override
                protected Void doInBackground() throws Exception {
                    repartition = moyenneService.repartitionMentions(classe.id());
                    moyennesParMatiere = moyenneService.moyennesParMatiere(classe.id());
                    double taux = moyenneService.tauxReussite(classe.id());
                    SwingUtilities.invokeLater(() ->
                        tauxReussiteLabel.setText(String.format(
                            "Taux de réussite : %.1f%%", taux * 100)));
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        PieChartPanel pie = new PieChartPanel(
                            "Répartition des mentions — " + classe.nom());
                        pie.setData(repartition);
                        chartsContainer.add(pie);
                        BarChartPanel bar = new BarChartPanel(
                            "Moyennes par matière — " + classe.nom());
                        bar.setData(moyennesParMatiere);
                        chartsContainer.add(bar);
                        chartsContainer.revalidate();
                        chartsContainer.repaint();
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Erreur génération graphiques", e);
                        chartsContainer.add(buildPlaceholder("Erreur graphiques"));
                    }
                }
            };
            worker.execute();
        }
    }

    private JPanel buildPlaceholder(String message) {
        JPanel panel = new JPanel();
        panel.add(new JLabel(message));
        return panel;
    }

    private JPanel buildStatCard(String label, String value) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        card.setBackground(new Color(240, 244, 249));

        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLabel = new JLabel(label, SwingConstants.CENTER);
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(Box.createVerticalGlue());
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(nameLabel);
        card.add(Box.createVerticalGlue());

        return card;
    }
}
