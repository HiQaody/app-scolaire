package com.monecole.gestion.views.matieres;

import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.dao.MatiereDao;
import com.monecole.gestion.models.Matiere;

import javax.swing.*;
import java.awt.*;

/**
 * Dialogue modal pour l'ajout et la modification d'une matière.
 */
public class MatiereFormDialog extends JDialog {

    private final JTextField codeField = new JTextField(15);
    private final JTextField libelleField = new JTextField(15);
    private final JTextField coefField = new JTextField(10);
    private final MatiereDao matiereDao;
    private final boolean isEdit;
    private Long matiereId;
    private boolean confirmed = false;

    public MatiereFormDialog(Window parent, Matiere matiere) {
        super(parent, matiere == null ? "Nouvelle matière" : "Modifier la matière", Dialog.ModalityType.APPLICATION_MODAL);
        this.isEdit = matiere != null;
        this.matiereDao = DaoFactory.getInstance().getMatiereDao();

        setLayout(new BorderLayout(10, 10));
        add(buildForm(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        if (isEdit && matiere != null) {
            this.matiereId = matiere.id();
            populateFields(matiere);
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
        panel.add(new JLabel("Code :"), gc);
        gc.gridx = 1;
        panel.add(codeField, gc);

        gc.gridx = 0; gc.gridy = 1;
        panel.add(new JLabel("Libellé :"), gc);
        gc.gridx = 1;
        panel.add(libelleField, gc);

        gc.gridx = 0; gc.gridy = 2;
        panel.add(new JLabel("Coefficient :"), gc);
        gc.gridx = 1;
        panel.add(coefField, gc);

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

    private void populateFields(Matiere matiere) {
        codeField.setText(matiere.code());
        libelleField.setText(matiere.libelle());
        coefField.setText(String.valueOf(matiere.coefficient()));
    }

    private void save() {
        String code = codeField.getText().trim();
        String libelle = libelleField.getText().trim();
        String coefStr = coefField.getText().trim();

        if (code.isEmpty() || libelle.isEmpty() || coefStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tous les champs obligatoires doivent être remplis.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double coefficient;
        try {
            coefficient = Double.parseDouble(coefStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Le coefficient doit être un nombre.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (isEdit) {
                Matiere updated = new Matiere(matiereId, code, libelle, coefficient);
                matiereDao.update(updated);
            } else {
                Matiere newMatiere = new Matiere(null, code, libelle, coefficient);
                matiereDao.create(newMatiere);
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
