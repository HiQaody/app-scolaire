package com.monecole.gestion.views.students;

import com.monecole.gestion.dao.ClasseDao;
import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.dao.EtudiantDao;
import com.monecole.gestion.models.Classe;
import com.monecole.gestion.models.Etudiant;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialogue modal pour l'ajout et la modification d'un étudiant.
 */
public class StudentFormDialog extends JDialog {

    private static final Logger LOGGER = Logger.getLogger(StudentFormDialog.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTextField matriculeField = new JTextField(15);
    private final JTextField nomField = new JTextField(15);
    private final JTextField prenomField = new JTextField(15);
    private final JTextField dateField = new JTextField(15);
    private final JComboBox<Etudiant.Sexe> sexeCombo;
    private JComboBox<Classe> classeCombo;
    private final EtudiantDao etudiantDao;
    private final boolean isEdit;
    private Long studentId;
    private boolean confirmed = false;

    public StudentFormDialog(Window parent, Etudiant student) {
        super(parent, student == null ? "Nouvel étudiant" : "Modifier l'étudiant", Dialog.ModalityType.APPLICATION_MODAL);
        this.isEdit = student != null;
        this.etudiantDao = DaoFactory.getInstance().getEtudiantDao();

        sexeCombo = new JComboBox<>(Etudiant.Sexe.values());
        ClasseDao classeDao = DaoFactory.getInstance().getClasseDao();
        try {
            List<Classe> classes = classeDao.findAll();
            classeCombo = new JComboBox<>(classes.toArray(new Classe[0]));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement classes", e);
            classeCombo = new JComboBox<>();
        }

        setLayout(new BorderLayout(10, 10));
        add(buildForm(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        if (isEdit && student != null) {
            this.studentId = student.id();
            populateFields(student);
        }

        pack();
        setLocationRelativeTo(parent);
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 5, 5, 5);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0; gc.gridy = 0;
        panel.add(new JLabel("Matricule :"), gc);
        gc.gridx = 1;
        panel.add(matriculeField, gc);

        gc.gridx = 0; gc.gridy = 1;
        panel.add(new JLabel("Nom :"), gc);
        gc.gridx = 1;
        panel.add(nomField, gc);

        gc.gridx = 0; gc.gridy = 2;
        panel.add(new JLabel("Prénom :"), gc);
        gc.gridx = 1;
        panel.add(prenomField, gc);

        gc.gridx = 0; gc.gridy = 3;
        panel.add(new JLabel("Date de naissance (dd/MM/yyyy) :"), gc);
        gc.gridx = 1;
        panel.add(dateField, gc);

        gc.gridx = 0; gc.gridy = 4;
        panel.add(new JLabel("Sexe :"), gc);
        gc.gridx = 1;
        panel.add(sexeCombo, gc);

        gc.gridx = 0; gc.gridy = 5;
        panel.add(new JLabel("Classe :"), gc);
        gc.gridx = 1;
        panel.add(classeCombo, gc);

        return panel;
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okBtn = new JButton(isEdit ? "Mettre à jour" : "Créer");
        okBtn.setFocusPainted(false);
        okBtn.addActionListener(e -> save());
        panel.add(okBtn);

        JButton cancelBtn = new JButton("Annuler");
        cancelBtn.setFocusPainted(false);
        cancelBtn.addActionListener(e -> dispose());
        panel.add(cancelBtn);

        return panel;
    }

    private void populateFields(Etudiant student) {
        matriculeField.setText(student.matricule());
        nomField.setText(student.nom());
        prenomField.setText(student.prenom());
        if (student.dateNaissance() != null) {
            dateField.setText(student.dateNaissance().format(DATE_FMT));
        }
        if (student.sexe() != null) {
            sexeCombo.setSelectedItem(student.sexe());
        }
        try {
            ClasseDao cd = DaoFactory.getInstance().getClasseDao();
            Classe c = cd.findById(student.idClasse());
            if (c != null) classeCombo.setSelectedItem(c);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur sélection classe", e);
        }
    }

    private void save() {
        String matricule = matriculeField.getText().trim();
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String dateStr = dateField.getText().trim();
        Etudiant.Sexe sexe = (Etudiant.Sexe) sexeCombo.getSelectedItem();
        Classe classe = (Classe) classeCombo.getSelectedItem();

        if (matricule.isEmpty() || nom.isEmpty() || prenom.isEmpty() || classe == null) {
            JOptionPane.showMessageDialog(this, "Tous les champs obligatoires doivent être remplis.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LocalDate dateNaissance = null;
        if (!dateStr.isEmpty()) {
            try {
                dateNaissance = LocalDate.parse(dateStr, DATE_FMT);
            } catch (DateTimeParseException e) {
                JOptionPane.showMessageDialog(this, "Format de date invalide (dd/MM/yyyy).", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        try {
            if (isEdit) {
                Etudiant updated = new Etudiant(studentId, matricule, nom, prenom, dateNaissance, sexe, classe.id());
                etudiantDao.update(updated);
            } else {
                Etudiant newStudent = new Etudiant(null, matricule, nom, prenom, dateNaissance, sexe, classe.id());
                etudiantDao.create(newStudent);
            }
            confirmed = true;
            dispose();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur sauvegarde étudiant", e);
            JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
