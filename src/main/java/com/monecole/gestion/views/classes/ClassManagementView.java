package com.monecole.gestion.views.classes;

import com.monecole.gestion.dao.ClasseDao;
import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.models.AnneeScolaire;
import com.monecole.gestion.models.Classe;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Vue de gestion des classes (CRUD).
 */
public class ClassManagementView extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(ClassManagementView.class.getName());

    private final ClasseDao classeDao;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private List<Classe> allClasses;

    public ClassManagementView() {
        this.classeDao = DaoFactory.getInstance().getClasseDao();
        this.tableModel = new DefaultTableModel(
            new Object[]{"ID", "Nom", "Niveau", "Année scolaire"}, 0) {};
        this.table = new JTable(tableModel) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

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

        JLabel title = new JLabel("Gestion des Classes");
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
        scroll.setBorder(BorderFactory.createTitledBorder("Liste des classes"));
        return scroll;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setOpaque(false);

        JButton addBtn = new JButton("+ Ajouter");
        addBtn.setFocusPainted(false);
        addBtn.addActionListener(e -> addClass());
        panel.add(addBtn);

        JButton editBtn = new JButton("✏ Modifier");
        editBtn.setFocusPainted(false);
        editBtn.addActionListener(e -> editClass());
        panel.add(editBtn);

        JButton delBtn = new JButton("🗑 Supprimer");
        delBtn.setFocusPainted(false);
        delBtn.addActionListener(e -> deleteClass());
        panel.add(delBtn);

        JButton refreshBtn = new JButton("↻ Actualiser");
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> loadClasses());
        panel.add(refreshBtn);

        return panel;
    }

    private void loadClasses() {
        try {
            allClasses = classeDao.findAll();
            updateTable(allClasses);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement classes", e);
            JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateTable(List<Classe> classes) {
        tableModel.setRowCount(0);
        for (Classe c : classes) {
            String anneeLabel = "—";
            try {
                AnneeScolaire a = DaoFactory.getInstance().getAnneeScolaireDao().findById(c.idAnnee());
                if (a != null) anneeLabel = a.libelle();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Erreur récupération année", ex);
            }
            tableModel.addRow(new Object[]{c.id(), c.nom(), c.niveau(), anneeLabel});
        }
    }

    private void addClass() {
        ClassFormDialog dialog = new ClassFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            loadClasses();
        }
    }

    private void editClass() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une classe.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Long id = (Long) table.getValueAt(selected, 0);
        try {
            Classe classe = classeDao.findById(id);
            if (classe != null) {
                ClassFormDialog dialog = new ClassFormDialog(SwingUtilities.getWindowAncestor(this), classe);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    loadClasses();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur édition classe", e);
        }
    }

    private void deleteClass() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une classe.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Long id = (Long) table.getValueAt(selected, 0);
        String nom = (String) table.getValueAt(selected, 1);

        int result = JOptionPane.showConfirmDialog(this,
            "Supprimer la classe \"" + nom + "\" ?",
            "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            try {
                classeDao.delete(id);
                loadClasses();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur suppression classe", e);
                JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
