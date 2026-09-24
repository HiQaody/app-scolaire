package com.monecole.gestion.views.classes;

import com.monecole.gestion.dao.AnneeScolaireDao;
import com.monecole.gestion.dao.ClasseDao;
import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.models.AnneeScolaire;
import com.monecole.gestion.models.Classe;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialogue modal pour l'ajout et la modification d'une classe.
 */
public class ClassFormDialog extends JDialog {

    private static final Logger LOGGER = Logger.getLogger(ClassFormDialog.class.getName());

    private final JTextField nomField = new JTextField(15);
    private final JTextField niveauField = new JTextField(15);
    private JComboBox<AnneeScolaire> anneeCombo;
    private final ClasseDao classeDao;
    private final boolean isEdit;
    private Long classId;
    private boolean confirmed = false;

    public ClassFormDialog(Window parent, Classe classe) {
        super(parent, classe == null ? "Nouvelle classe" : "Modifier la classe", Dialog.ModalityType.APPLICATION_MODAL);
        this.isEdit = classe != null;
        this.classeDao = DaoFactory.getInstance().getClasseDao();

        AnneeScolaireDao anneeDao = DaoFactory.getInstance().getAnneeScolaireDao();
        try {
            List<AnneeScolaire> annees = anneeDao.findAll();
            anneeCombo = new JComboBox<>(annees.toArray(new AnneeScolaire[0]));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement années scolaires", e);
            anneeCombo = new JComboBox<>();
        }

        setLayout(new BorderLayout(10, 10));
        add(buildForm(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        if (isEdit && classe != null) {
            this.classId = classe.id();
            populateFields(classe);
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
        panel.add(new JLabel("Nom :"), gc);
        gc.gridx = 1;
        panel.add(nomField, gc);

        gc.gridx = 0; gc.gridy = 1;
        panel.add(new JLabel("Niveau :"), gc);
        gc.gridx = 1;
        panel.add(niveauField, gc);

        gc.gridx = 0; gc.gridy = 2;
        panel.add(new JLabel("Année scolaire :"), gc);
        gc.gridx = 1;
        panel.add(anneeCombo, gc);

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

    private void populateFields(Classe classe) {
        nomField.setText(classe.nom());
        niveauField.setText(classe.niveau());
        try {
            AnneeScolaireDao ad = DaoFactory.getInstance().getAnneeScolaireDao();
            AnneeScolaire a = ad.findById(classe.idAnnee());
            if (a != null) anneeCombo.setSelectedItem(a);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur sélection année", e);
        }
    }

    private void save() {
        String nom = nomField.getText().trim();
        String niveau = niveauField.getText().trim();
        AnneeScolaire annee = (AnneeScolaire) anneeCombo.getSelectedItem();

        if (nom.isEmpty() || annee == null) {
            JOptionPane.showMessageDialog(this, "Tous les champs obligatoires doivent être remplis.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (isEdit) {
                Classe updated = new Classe(classId, nom, niveau, annee.id());
                classeDao.update(updated);
            } else {
                Classe newClasse = new Classe(null, nom, niveau, annee.id());
                classeDao.create(newClasse);
            }
            confirmed = true;
            dispose();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur sauvegarde classe", e);
            JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
