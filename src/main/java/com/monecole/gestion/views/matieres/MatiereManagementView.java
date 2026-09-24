package com.monecole.gestion.views.matieres;

import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.dao.MatiereDao;
import com.monecole.gestion.models.Matiere;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Vue de gestion des matières (CRUD).
 */
public class MatiereManagementView extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(MatiereManagementView.class.getName());

    private final MatiereDao matiereDao;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private List<Matiere> allMatieres;

    public MatiereManagementView() {
        this.matiereDao = DaoFactory.getInstance().getMatiereDao();
        this.tableModel = new DefaultTableModel(
            new Object[]{"ID", "Code", "Libellé", "Coefficient"}, 0) {};
        this.table = new JTable(tableModel) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTableWrapper(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);

        loadMatieres();
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel title = new JLabel("Gestion des Matières");
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
        scroll.setBorder(BorderFactory.createTitledBorder("Liste des matières"));
        return scroll;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setOpaque(false);

        JButton addBtn = new JButton("+ Ajouter");
        addBtn.setFocusPainted(false);
        addBtn.addActionListener(e -> addMatiere());
        panel.add(addBtn);

        JButton editBtn = new JButton("✏ Modifier");
        editBtn.setFocusPainted(false);
        editBtn.addActionListener(e -> editMatiere());
        panel.add(editBtn);

        JButton delBtn = new JButton("🗑 Supprimer");
        delBtn.setFocusPainted(false);
        delBtn.addActionListener(e -> deleteMatiere());
        panel.add(delBtn);

        JButton refreshBtn = new JButton("↻ Actualiser");
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> loadMatieres());
        panel.add(refreshBtn);

        return panel;
    }

    private void loadMatieres() {
        try {
            allMatieres = matiereDao.findAll();
            updateTable(allMatieres);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement matières", e);
            JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateTable(List<Matiere> matieres) {
        tableModel.setRowCount(0);
        for (Matiere m : matieres) {
            tableModel.addRow(new Object[]{m.id(), m.code(), m.libelle(), m.coefficient()});
        }
    }

    private void addMatiere() {
        MatiereFormDialog dialog = new MatiereFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            loadMatieres();
        }
    }

    private void editMatiere() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une matière.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Long id = (Long) table.getValueAt(table.convertRowIndexToModel(selected), 0);
        try {
            Matiere matiere = matiereDao.findById(id);
            if (matiere != null) {
                MatiereFormDialog dialog = new MatiereFormDialog(SwingUtilities.getWindowAncestor(this), matiere);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    loadMatieres();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur édition matière", e);
        }
    }

    private void deleteMatiere() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une matière.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Long id = (Long) table.getValueAt(table.convertRowIndexToModel(selected), 0);
        String code = (String) table.getValueAt(table.convertRowIndexToModel(selected), 1);
        String libelle = (String) table.getValueAt(table.convertRowIndexToModel(selected), 2);

        int result = JOptionPane.showConfirmDialog(this,
            "Supprimer la matière " + code + " (" + libelle + ") ?",
            "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            try {
                matiereDao.delete(id);
                loadMatieres();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur suppression matière", e);
                JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
