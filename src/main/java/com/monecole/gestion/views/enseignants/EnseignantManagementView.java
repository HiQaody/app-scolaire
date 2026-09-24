package com.monecole.gestion.views.enseignants;

import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.dao.EnseignantDao;
import com.monecole.gestion.models.Enseignant;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Vue de gestion des enseignants (CRUD).
 */
public class EnseignantManagementView extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(EnseignantManagementView.class.getName());

    private final EnseignantDao enseignantDao;
    private final DefaultTableModel tableModel;
    private final JTable table;

    public EnseignantManagementView() {
        this.enseignantDao = DaoFactory.getInstance().getEnseignantDao();
        this.tableModel = new DefaultTableModel(
            new Object[]{"ID", "Matricule", "Nom", "Prénom"}, 0) {};
        this.table = new JTable(tableModel) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTableWrapper(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);

        loadEnseignants();
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel title = new JLabel("Gestion des Enseignants");
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
        scroll.setBorder(BorderFactory.createTitledBorder("Liste des enseignants"));
        return scroll;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setOpaque(false);

        JButton addBtn = new JButton("+ Ajouter");
        addBtn.setFocusPainted(false);
        addBtn.addActionListener(e -> addEnseignant());
        panel.add(addBtn);

        JButton editBtn = new JButton("✏ Modifier");
        editBtn.setFocusPainted(false);
        editBtn.addActionListener(e -> editEnseignant());
        panel.add(editBtn);

        JButton delBtn = new JButton("🗑 Supprimer");
        delBtn.setFocusPainted(false);
        delBtn.addActionListener(e -> deleteEnseignant());
        panel.add(delBtn);

        JButton refreshBtn = new JButton("↻ Actualiser");
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> loadEnseignants());
        panel.add(refreshBtn);

        return panel;
    }

    private void loadEnseignants() {
        try {
            List<Enseignant> enseignants = enseignantDao.findAll();
            tableModel.setRowCount(0);
            for (Enseignant e : enseignants) {
                tableModel.addRow(new Object[]{e.id(), e.matricule(), e.nom(), e.prenom()});
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement enseignants", e);
            JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addEnseignant() {
        EnseignantFormDialog dialog = new EnseignantFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            loadEnseignants();
        }
    }

    private void editEnseignant() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner un enseignant.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Long id = (Long) table.getValueAt(table.convertRowIndexToModel(selected), 0);
        try {
            Enseignant ens = enseignantDao.findById(id);
            if (ens != null) {
                EnseignantFormDialog dialog = new EnseignantFormDialog(SwingUtilities.getWindowAncestor(this), ens);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    loadEnseignants();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur édition enseignant", e);
        }
    }

    private void deleteEnseignant() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner un enseignant.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Long id = (Long) table.getValueAt(table.convertRowIndexToModel(selected), 0);
        String nom = (String) table.getValueAt(table.convertRowIndexToModel(selected), 2);
        String prenom = (String) table.getValueAt(table.convertRowIndexToModel(selected), 3);

        int result = JOptionPane.showConfirmDialog(this,
            "Supprimer l'enseignant " + nom + " " + prenom + " ?",
            "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            try {
                enseignantDao.delete(id);
                loadEnseignants();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur suppression enseignant", e);
                JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
