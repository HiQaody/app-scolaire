package com.monecole.gestion.views.enseignants;

import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.dao.EnseignantDao;
import com.monecole.gestion.models.Enseignant;

import javax.swing.*;
import java.awt.*;

/**
 * Dialogue modal pour l'ajout et la modification d'un enseignant.
 */
public class EnseignantFormDialog extends JDialog {

    private final JTextField matriculeField = new JTextField(15);
    private final JTextField nomField = new JTextField(15);
    private final JTextField prenomField = new JTextField(15);
    private final EnseignantDao enseignantDao;
    private final boolean isEdit;
    private Long enseignantId;
    private boolean confirmed = false;

    public EnseignantFormDialog(Window parent, Enseignant enseignant) {
        super(parent, enseignant == null ? "Nouvel enseignant" : "Modifier l'enseignant", Dialog.ModalityType.APPLICATION_MODAL);
        this.isEdit = enseignant != null;
        this.enseignantDao = DaoFactory.getInstance().getEnseignantDao();

        setLayout(new BorderLayout(10, 10));
        add(buildForm(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        if (isEdit && enseignant != null) {
            this.enseignantId = enseignant.id();
            populateFields(enseignant);
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

    private void populateFields(Enseignant enseignant) {
        matriculeField.setText(enseignant.matricule());
        nomField.setText(enseignant.nom());
        prenomField.setText(enseignant.prenom());
    }

    private void save() {
        String matricule = matriculeField.getText().trim();
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();

        if (matricule.isEmpty() || nom.isEmpty() || prenom.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tous les champs obligatoires doivent être remplis.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (isEdit) {
                Enseignant updated = new Enseignant(enseignantId, matricule, nom, prenom);
                enseignantDao.update(updated);
            } else {
                Enseignant newEns = new Enseignant(null, matricule, nom, prenom);
                enseignantDao.create(newEns);
            }
            confirmed = true;
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
