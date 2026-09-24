package com.monecole.gestion.views.reports;

import com.monecole.gestion.dao.ClasseDao;
import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.dao.EtudiantDao;
import com.monecole.gestion.models.Classe;
import com.monecole.gestion.models.Etudiant;
import com.monecole.gestion.services.BulletinService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Vue "Bulletins PDF" (Sprint 5) : sélection d'un étudiant puis génération du bulletin PDF.
 */
public class BulletinView extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(BulletinView.class.getName());

    private final ClasseDao classeDao;
    private final EtudiantDao etudiantDao;
    private final BulletinService bulletinService;

    private JComboBox<Classe> classeCombo;
    private JComboBox<Etudiant> etudiantCombo;
    private JComboBox<String> trimestreCombo;
    private DefaultTableModel tableModel;
    private JTable table;
    private JButton generateBtn;
    private boolean generationEnCours = false;

    public BulletinView() {
        DaoFactory df = DaoFactory.getInstance();
        this.classeDao = df.getClasseDao();
        this.etudiantDao = df.getEtudiantDao();
        this.bulletinService = new BulletinService();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTableWrapper(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);

        loadClasses();
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel title = new JLabel("Bulletins PDF");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        panel.add(title, BorderLayout.WEST);

        return panel;
    }

    private JScrollPane buildTableWrapper() {
        tableModel = new DefaultTableModel(new Object[]{"ID", "Matricule", "Nom", "Prénom"}, 0) {};
        table = new JTable(tableModel) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table.setFillsViewportHeight(true);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Étudiants de la classe sélectionnée"));
        return scroll;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setOpaque(false);

        panel.add(new JLabel("Classe :"));
        classeCombo = new JComboBox<>();
        classeCombo.setPreferredSize(new Dimension(160, 30));
        classeCombo.addActionListener(e -> loadEtudiants());
        panel.add(classeCombo);

        panel.add(new JLabel("Étudiant :"));
        etudiantCombo = new JComboBox<>();
        etudiantCombo.setPreferredSize(new Dimension(220, 30));
        panel.add(etudiantCombo);

        panel.add(new JLabel("Période :"));
        trimestreCombo = new JComboBox<>(new String[]{
            "Année complète", "Trimestre 1", "Trimestre 2", "Trimestre 3"});
        trimestreCombo.setPreferredSize(new Dimension(150, 30));
        panel.add(trimestreCombo);

        JButton refreshBtn = new JButton("↻ Actualiser");
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> loadClasses());
        panel.add(refreshBtn);

        generateBtn = new JButton("📄 Générer le bulletin PDF");
        generateBtn.setFocusPainted(false);
        generateBtn.addActionListener(e -> genererBulletin());
        panel.add(generateBtn);

        return panel;
    }

    private void loadClasses() {
        try {
            classeCombo.removeAllItems();
            List<Classe> classes = classeDao.findAll();
            for (Classe c : classes) {
                classeCombo.addItem(c);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement classes", e);
            JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadEtudiants() {
        try {
            etudiantCombo.removeAllItems();
            Classe selected = (Classe) classeCombo.getSelectedItem();
            if (selected == null) return;
            tableModel.setRowCount(0);
            List<Etudiant> etudiants = etudiantDao.findByIdClasse(selected.id());
            for (Etudiant etu : etudiants) {
                etudiantCombo.addItem(etu);
                tableModel.addRow(new Object[]{etu.id(), etu.matricule(), etu.nom(), etu.prenom()});
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement étudiants", e);
            JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void genererBulletin() {
        if (generationEnCours) return;

        final Etudiant etudiant = (Etudiant) etudiantCombo.getSelectedItem();
        if (etudiant == null) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner un étudiant.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        final String trimestre = (String) trimestreCombo.getSelectedItem();

        setBusy(true);
        new SwingWorker<Path, Void>() {
            @Override protected Path doInBackground() throws Exception {
                return bulletinService.genererBulletin(etudiant, trimestre);
            }
            @Override protected void done() {
                setBusy(false);
                try {
                    Path pdf = get();
                    int choice = JOptionPane.showConfirmDialog(BulletinView.this,
                        "Bulletin généré avec succès :\n" + pdf.toAbsolutePath()
                            + "\n\nOuvrir le fichier ?",
                        "Succès", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    if (choice == JOptionPane.YES_OPTION) {
                        ouvrirPdf(pdf);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur génération bulletin", e);
                    Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                    JOptionPane.showMessageDialog(BulletinView.this,
                        "Erreur lors de la génération : " + cause.getMessage(),
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void ouvrirPdf(Path pdf) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(pdf.toFile());
            } else {
                JOptionPane.showMessageDialog(this, "Ouvrez manuellement : " + pdf.toAbsolutePath(),
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Ouverture PDF impossible", e);
            JOptionPane.showMessageDialog(this, "Impossible d'ouvrir le PDF : " + e.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setBusy(boolean busy) {
        generationEnCours = busy;
        generateBtn.setEnabled(!busy);
        generateBtn.setText(busy ? "⏳ Génération..." : "📄 Générer le bulletin PDF");
    }
}
