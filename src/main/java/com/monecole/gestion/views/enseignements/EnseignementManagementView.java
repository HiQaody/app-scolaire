package com.monecole.gestion.views.enseignements;

import com.monecole.gestion.dao.*;
import com.monecole.gestion.models.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Vue de gestion des enseignements (liaison Enseignant-Matière-Classe).
 */
public class EnseignementManagementView extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(EnseignementManagementView.class.getName());

    private final EnseignementDao enseignementDao;
    private final DefaultTableModel tableModel;
    private final JTable table;

    public EnseignementManagementView() {
        this.enseignementDao = DaoFactory.getInstance().getEnseignementDao();
        this.tableModel = new DefaultTableModel(
            new Object[]{"ID", "Enseignant", "Matière", "Classe"}, 0) {};
        this.table = new JTable(tableModel) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTableWrapper(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);

        loadEnseignements();
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel title = new JLabel("Gestion des Enseignements");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        panel.add(title, BorderLayout.WEST);
        return panel;
    }

    private JScrollPane buildTableWrapper() {
        table.setFillsViewportHeight(true);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Liaisons Enseignant ↔ Matière ↔ Classe"));
        return scroll;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setOpaque(false);

        JButton addBtn = new JButton("+ Assigner");
        addBtn.setFocusPainted(false);
        addBtn.addActionListener(e -> assignEnseignement());
        panel.add(addBtn);

        JButton delBtn = new JButton("🗑 Supprimer");
        delBtn.setFocusPainted(false);
        delBtn.addActionListener(e -> unassignEnseignement());
        panel.add(delBtn);

        JButton refreshBtn = new JButton("↻ Actualiser");
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> loadEnseignements());
        panel.add(refreshBtn);

        return panel;
    }

    private void loadEnseignements() {
        try {
            DaoFactory df = DaoFactory.getInstance();
            List<Enseignement> enseignements = enseignementDao.findAll();

            tableModel.setRowCount(0);
            for (Enseignement ens : enseignements) {
                String enseignantName = getEnseignantName(df, ens.idEnseignant());
                String matiereName = getMatiereName(df, ens.idMatiere());
                String classeName = getClasseName(df, ens.idClasse());
                tableModel.addRow(new Object[]{ens.id(), enseignantName, matiereName, classeName});
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement enseignements", e);
        }
    }

    private String getEnseignantName(DaoFactory df, Long id) {
        try {
            Enseignant e = df.getEnseignantDao().findById(id);
            return e != null ? e.nom() + " " + e.prenom() : "?";
        } catch (Exception ex) {
            return "—";
        }
    }

    private String getMatiereName(DaoFactory df, Long id) {
        try {
            Matiere m = df.getMatiereDao().findById(id);
            return m != null ? m.libelle() : "?";
        } catch (Exception ex) {
            return "—";
        }
    }

    private String getClasseName(DaoFactory df, Long id) {
        try {
            Classe c = df.getClasseDao().findById(id);
            return c != null ? c.nom() : "?";
        } catch (Exception ex) {
            return "—";
        }
    }

    private void assignEnseignement() {
        EnseignementAssignmentDialog dialog = new EnseignementAssignmentDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            loadEnseignements();
        }
    }

    private void unassignEnseignement() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une liaison.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Long id = (Long) table.getValueAt(table.convertRowIndexToModel(selected), 0);

        int result = JOptionPane.showConfirmDialog(this,
            "Supprimer cette liaison enseignant-matiereclasse ?",
            "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            try {
                enseignementDao.delete(id);
                loadEnseignements();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur suppression enseignement", e);
                JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
