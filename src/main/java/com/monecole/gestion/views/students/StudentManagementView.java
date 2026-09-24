package com.monecole.gestion.views.students;

import com.monecole.gestion.dao.ClasseDao;
import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.dao.EtudiantDao;
import com.monecole.gestion.models.Classe;
import com.monecole.gestion.models.Etudiant;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Vue de gestion des étudiants (CRUD).
 * Affiche la liste dans un JTable avec tri et filtres.
 */
public class StudentManagementView extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(StudentManagementView.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EtudiantDao etudiantDao;
    private final ClasseDao classeDao;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField searchField;
    private List<Etudiant> allStudents;

    public StudentManagementView() {
        this.etudiantDao = DaoFactory.getInstance().getEtudiantDao();
        this.classeDao = DaoFactory.getInstance().getClasseDao();
        this.tableModel = new DefaultTableModel(
            new Object[]{"ID", "Matricule", "Nom", "Prénom", "Date de naissance", "Sexe", "Classe"}, 0) {};
        this.table = new JTable(tableModel) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        this.searchField = new JTextField(20);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTableWrapper(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);

        loadStudents();
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel title = new JLabel("Gestion des Étudiants");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        panel.add(title, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        searchPanel.setOpaque(false);
        searchPanel.add(new JLabel("Recherche :"));
        searchField.setToolTipText("Filtrer par nom, prénom ou matricule");
        searchPanel.add(searchField);
        JButton searchBtn = new JButton("🔍");
        searchBtn.setFocusPainted(false);
        searchBtn.addActionListener(e -> filterStudents());
        searchPanel.add(searchBtn);
        panel.add(searchPanel, BorderLayout.EAST);

        return panel;
    }

    private JScrollPane buildTableWrapper() {
        table.setFillsViewportHeight(true);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Liste des étudiants"));
        return scroll;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setOpaque(false);

        JButton addBtn = new JButton("+ Ajouter");
        addBtn.setFocusPainted(false);
        addBtn.addActionListener(e -> addStudent());
        panel.add(addBtn);

        JButton editBtn = new JButton("✏ Modifier");
        editBtn.setFocusPainted(false);
        editBtn.addActionListener(e -> editStudent());
        panel.add(editBtn);

        JButton delBtn = new JButton("🗑 Supprimer");
        delBtn.setFocusPainted(false);
        delBtn.addActionListener(e -> deleteStudent());
        panel.add(delBtn);

        JButton refreshBtn = new JButton("↻ Actualiser");
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> loadStudents());
        panel.add(refreshBtn);

        return panel;
    }

    private void loadStudents() {
        try {
            allStudents = etudiantDao.findAll();
            updateTable(allStudents);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement étudiants", e);
            JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateTable(List<Etudiant> students) {
        tableModel.setRowCount(0);
        for (Etudiant e : students) {
            String classeNom = "";
            try {
                Classe c = classeDao.findById(e.idClasse());
                if (c != null) classeNom = c.nom();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Erreur récupération classe", ex);
            }
            String dateStr = e.dateNaissance() != null ? e.dateNaissance().format(DATE_FMT) : "—";
            tableModel.addRow(new Object[]{
                e.id(), e.matricule(), e.nom(), e.prenom(), dateStr,
                e.sexe() != null ? e.sexe().name() : "—", classeNom
            });
        }
    }

    private void filterStudents() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            updateTable(allStudents);
            return;
        }
        List<Etudiant> filtered = allStudents.stream()
            .filter(e -> e.nom().toLowerCase().contains(query)
                || e.prenom().toLowerCase().contains(query)
                || e.matricule().toLowerCase().contains(query))
            .toList();
        updateTable(filtered);
    }

    private void addStudent() {
        StudentFormDialog dialog = new StudentFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            loadStudents();
        }
    }

    private void editStudent() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner un étudiant.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Long id = (Long) table.getValueAt(selected, 0);
        try {
            Etudiant student = etudiantDao.findById(id);
            if (student != null) {
                StudentFormDialog dialog = new StudentFormDialog(SwingUtilities.getWindowAncestor(this), student);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    loadStudents();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur édition étudiant", e);
        }
    }

    private void deleteStudent() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner un étudiant.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Long id = (Long) table.getValueAt(selected, 0);
        String matricule = (String) table.getValueAt(selected, 1);
        String nom = (String) table.getValueAt(selected, 2);

        int result = JOptionPane.showConfirmDialog(this,
            "Supprimer l'étudiant " + nom + " (" + matricule + ") ?",
            "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            try {
                etudiantDao.delete(id);
                loadStudents();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur suppression étudiant", e);
                JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
