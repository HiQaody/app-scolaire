package com.monecole.gestion.views.grades;

import com.monecole.gestion.dao.*;
import com.monecole.gestion.models.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Vue de saisie des notes pour un enseignement donné.
 * Affiche les étudiants d'une classe avec une JTable éditable pour les notes.
 * Utilise SwingWorker pour les calculs asynchrones.
 */
public class NoteManagementView extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(NoteManagementView.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EtudiantDao etudiantDao;
    private final EnseignementDao enseignementDao;
    private final EvaluationDao evaluationDao;
    private final NoteDao noteDao;
    private final MatiereDao matiereDao;
    private final EnseignantDao enseignantDao;
    private final ClasseDao classeDao;

    private JComboBox<String> enseignementCombo;
    private JComboBox<String> evaluationCombo;
    private JTable noteTable;
    private DefaultTableModel tableModel;
    private JButton saveBtn;
    private JLabel averageLabel;

    private Map<Long, Etudiant> etudiantCache = new HashMap<>();
    private boolean isLocked = false;

    public NoteManagementView() {
        DaoFactory df = DaoFactory.getInstance();
        this.etudiantDao = df.getEtudiantDao();
        this.enseignementDao = df.getEnseignementDao();
        this.evaluationDao = df.getEvaluationDao();
        this.noteDao = df.getNoteDao();
        this.matiereDao = df.getMatiereDao();
        this.enseignantDao = df.getEnseignantDao();
        this.classeDao = df.getClasseDao();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildToolbar(), BorderLayout.WEST);
        add(buildTableWrapper(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel title = new JLabel("Gestion des Notes");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        panel.add(title, BorderLayout.WEST);
        return panel;
    }

    private JPanel buildToolbar() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        panel.setOpaque(false);

        panel.add(new JLabel("Enseignement :"));
        enseignementCombo = new JComboBox<>(new String[]{"Sélectionner..."});
        enseignementCombo.setToolTipText("Sélectionner un enseignement (matière + classe)");
        enseignementCombo.addActionListener(e -> onEnseignementChanged());
        panel.add(enseignementCombo);

        panel.add(new JLabel("Évaluation :"));
        evaluationCombo = new JComboBox<>(new String[]{"Sélectionner..."});
        evaluationCombo.setToolTipText("Sélectionner une évaluation");
        evaluationCombo.addActionListener(e -> onEvaluationChanged());
        panel.add(evaluationCombo);

        saveBtn = new JButton("💾 Enregistrer les notes");
        saveBtn.setFocusPainted(false);
        saveBtn.addActionListener(e -> saveNotes());
        saveBtn.setEnabled(false);
        panel.add(saveBtn);

        loadEnseignements();

        return panel;
    }

    private JScrollPane buildTableWrapper() {
        tableModel = new DefaultTableModel(
            new Object[]{"Étudiant", "Note", "Commentaire"}, 0) {};
        noteTable = new JTable(tableModel) {
            @Override public boolean isCellEditable(int row, int col) {
                return isLocked ? false : col == 1 || col == 2;
            }
            @Override public TableCellRenderer getCellRenderer(int row, int column) {
                if (column == 1) return new NoteCellRenderer();
                return super.getCellRenderer(row, column);
            }
            @Override public TableCellEditor getCellEditor(int row, int column) {
                if (column == 1) return new NoteCellEditor();
                return super.getCellEditor(row, column);
            }
        };
        noteTable.setFillsViewportHeight(true);
        noteTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane scroll = new JScrollPane(noteTable);
        scroll.setBorder(BorderFactory.createTitledBorder("Saisie des notes par étudiant"));
        return scroll;
    }

    private JPanel buildFooter() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        averageLabel = new JLabel("Moyenne de la classe : —");
        averageLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(averageLabel, BorderLayout.WEST);

        JButton lockBtn = new JButton("🔒 Verrouiller / Déverrouiller");
        lockBtn.setFocusPainted(false);
        lockBtn.addActionListener(e -> toggleLock());
        panel.add(lockBtn, BorderLayout.EAST);

        return panel;
    }

    private void loadEnseignements() {
        enseignementCombo.removeAllItems();
        enseignementCombo.addItem("Sélectionner...");

        try {
            List<Enseignement> ens = enseignementDao.findByIdEnseignant(
                DaoFactory.getInstance().getUtilisateurDao()
                    .findByLogin("prof.math").idEnseignant()
            );
            for (Enseignement e : ens) {
                String display = formatEnseignement(e);
                enseignementCombo.addItem(display);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur chargement enseignements", e);
        }
    }

    private String formatEnseignement(Enseignement e) {
        try {
            String matiere = matiereDao.findById(e.idMatiere()).libelle();
            String classe = classeDao.findById(e.idClasse()).nom();
            return matiere + " - " + classe + " [id=" + e.id() + "]";
        } catch (Exception ex) {
            return "? - ? [id=" + e.id() + "]";
        }
    }

    private void onEnseignementChanged() {
        tableModel.setRowCount(0);
        evaluationCombo.removeAllItems();
        evaluationCombo.addItem("Sélectionner...");
        saveBtn.setEnabled(false);
        averageLabel.setText("Moyenne de la classe : —");
        isLocked = false;

        String selected = (String) enseignementCombo.getSelectedItem();
        if (selected == null || !selected.contains("id=")) return;

        Long ensId = extractId(selected);
        if (ensId == null) return;

        try {
            List<Evaluation> evals = evaluationDao.findByIdEnseignement(ensId);
            for (Evaluation ev : evals) {
                String display = ev.type().getLabel() + " - " + ev.libelle()
                    + " (" + (ev.date() != null ? ev.date().format(DATE_FMT) : "—") + ") [id=" + ev.id() + "]";
                evaluationCombo.addItem(display);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur chargement évaluations", e);
        }
    }

    private void onEvaluationChanged() {
        tableModel.setRowCount(0);
        saveBtn.setEnabled(false);
        averageLabel.setText("Moyenne de la classe : —");
        isLocked = false;

        String selected = (String) evaluationCombo.getSelectedItem();
        if (selected == null || !selected.contains("id=")) return;

        Long evalId = extractId(selected);
        if (evalId == null) return;

        loadStudentsForEvaluation(evalId);
    }

    private void loadStudentsForEvaluation(Long evalId) {
        SwingWorker<List<Etudiant>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Etudiant> doInBackground() throws Exception {
                Evaluation ev = evaluationDao.findById(evalId);
                if (ev == null) return List.of();
                Enseignement ens = enseignementDao.findById(ev.idEnseignement());
                if (ens == null) return List.of();
                return etudiantDao.findByIdClasse(ens.idClasse());
            }

            @Override
            protected void done() {
                try {
                    List<Etudiant> students = get();
                    etudiantCache.clear();
                    saveBtn.setEnabled(!students.isEmpty());
                    for (Etudiant e : students) {
                        etudiantCache.put(e.id(), e);
                        try {
                            Note note = noteDao.findByIdEtudiantAndEvaluation(e.id(), evalId);
                            Double value = (note != null) ? note.valeur() : null;
                            tableModel.addRow(new Object[]{
                                e.nom() + " " + e.prenom() + " (" + e.matricule() + ")",
                                value,
                                ""
                            });
                        } catch (Exception ex) {
                            tableModel.addRow(new Object[]{
                                e.nom() + " " + e.prenom() + " (" + e.matricule() + ")",
                                null, ""
                            });
                        }
                    }
                    computeAverage();
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Erreur chargement étudiants notes", ex);
                }
            }
        };
        worker.execute();
    }

    private void saveNotes() {
        String selected = (String) evaluationCombo.getSelectedItem();
        if (selected == null || !selected.contains("id=")) return;
        Long evalId = extractId(selected);
        if (evalId == null) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                DaoFactory df = DaoFactory.getInstance();
                df.getConnection().setAutoCommit(false);
                try {
                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        Double value = (Double) tableModel.getValueAt(i, 1);
                        Long etudiantId = getStudentIdFromRow(i);
                        if (etudiantId != null) {
                            Note existing = noteDao.findByIdEtudiantAndEvaluation(etudiantId, evalId);
                            if (existing != null) {
                                noteDao.update(new Note(existing.id(), value, etudiantId, evalId));
                            } else if (value != null) {
                                noteDao.create(new Note(null, value, etudiantId, evalId));
                            }
                        }
                    }
                    df.getConnection().commit();
                } catch (Exception e) {
                    df.getConnection().rollback();
                    throw e;
                } finally {
                    df.getConnection().setAutoCommit(true);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(NoteManagementView.this,
                        "Notes enregistrées avec succès.", "Succès", JOptionPane.INFORMATION_MESSAGE);
                    computeAverage();
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Erreur sauvegarde notes", ex);
                    JOptionPane.showMessageDialog(NoteManagementView.this,
                        "Erreur : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void computeAverage() {
        SwingWorker<Double, Void> worker = new SwingWorker<>() {
            @Override
            protected Double doInBackground() {
                List<Double> values = new ArrayList<>();
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    Object val = tableModel.getValueAt(i, 1);
                    if (val instanceof Double d && d != null) {
                        values.add(d);
                    }
                }
                return values.isEmpty() ? null : values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            }

            @Override
            protected void done() {
                try {
                    Double avg = get();
                    if (avg != null) {
                        averageLabel.setText(String.format("Moyenne de la classe : %.2f", avg));
                    } else {
                        averageLabel.setText("Moyenne de la classe : —");
                    }
                } catch (Exception e) {
                    averageLabel.setText("Erreur calcul moyenne");
                }
            }
        };
        worker.execute();
    }

    private Long getStudentIdFromRow(int rowIdx) {
        int cacheIdx = 0;
        for (Long id : etudiantCache.keySet()) {
            if (cacheIdx == rowIdx) return id;
            cacheIdx++;
        }
        return null;
    }

    private Long extractId(String display) {
        try {
            return Long.parseLong(display.replaceAll(".*\\[id=(\\d+)\\].*", "$1"));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void toggleLock() {
        isLocked = !isLocked;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.fireTableCellUpdated(i, 1);
        }
    }

    private static class NoteCellRenderer extends JTextField implements TableCellRenderer {
        public NoteCellRenderer() { setHorizontalAlignment(JTextField.CENTER); }
        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            setText(value == null ? "" : value.toString());
            setBackground(value == null ? Color.LIGHT_GRAY : Color.WHITE);
            return this;
        }
    }

    private static class NoteCellEditor extends DefaultCellEditor {
        private final JTextField field;
        public NoteCellEditor() {
            super(new JTextField());
            field = (JTextField) editorComponent;
            field.setHorizontalAlignment(JTextField.CENTER);
        }
        @Override public Object getCellEditorValue() {
            try {
                return Double.parseDouble(field.getText());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        @Override public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            field.setText(value == null ? "" : value.toString());
            return field;
        }
    }
}
