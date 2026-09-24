package com.monecole.gestion.views.enseignements;

import com.monecole.gestion.dao.*;
import com.monecole.gestion.models.Classe;
import com.monecole.gestion.models.Enseignant;
import com.monecole.gestion.models.Enseignement;
import com.monecole.gestion.models.Matiere;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialogue de création d'une liaison Enseignant-Matière-Classe.
 */
public class EnseignementAssignmentDialog extends JDialog {

    private static final Logger LOGGER = Logger.getLogger(EnseignementAssignmentDialog.class.getName());

    private final JComboBox<String> enseignantCombo;
    private final JComboBox<String> matiereCombo;
    private final JComboBox<String> classeCombo;
    private final EnseignementDao enseignementDao;
    private boolean confirmed = false;

    public EnseignementAssignmentDialog(Window parent) {
        super(parent, "Assigner un enseignement", Dialog.ModalityType.APPLICATION_MODAL);
        this.enseignementDao = DaoFactory.getInstance().getEnseignementDao();

        DaoFactory df = DaoFactory.getInstance();
        List<Enseignant> enseignants;
        List<Matiere> matieres;
        List<Classe> classes;
        try {
            enseignants = df.getEnseignantDao().findAll();
            matieres = df.getMatiereDao().findAll();
            classes = df.getClasseDao().findAll();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur chargement données", e);
            enseignants = List.of();
            matieres = List.of();
            classes = List.of();
        }

        enseignantCombo = new JComboBox<>(enseignants.stream()
            .map(e -> e.id() + " - " + e.nom() + " " + e.prenom())
            .toArray(String[]::new));
        matiereCombo = new JComboBox<>(matieres.stream()
            .map(m -> m.id() + " - " + m.libelle())
            .toArray(String[]::new));
        classeCombo = new JComboBox<>(classes.stream()
            .map(c -> c.id() + " - " + c.nom())
            .toArray(String[]::new));

        setLayout(new BorderLayout(10, 10));
        add(buildForm(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        pack();
        setSize(400, 200);
        setLocationRelativeTo(parent);
    }

    
    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 5, 5, 5);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        gc.gridx = 0; gc.gridy = 0;
        panel.add(new JLabel("Enseignant :"), gc);
        gc.gridx = 1;
        panel.add(enseignantCombo, gc);

        gc.gridx = 0; gc.gridy = 1;
        panel.add(new JLabel("Matière :"), gc);
        gc.gridx = 1;
        panel.add(matiereCombo, gc);

        gc.gridx = 0; gc.gridy = 2;
        panel.add(new JLabel("Classe :"), gc);
        gc.gridx = 1;
        panel.add(classeCombo, gc);

        return panel;
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okBtn = new JButton("Assigner");
        okBtn.setFocusPainted(false);
        okBtn.addActionListener(e -> save());
        panel.add(okBtn);

        JButton cancelBtn = new JButton("Annuler");
        cancelBtn.setFocusPainted(false);
        cancelBtn.addActionListener(e -> dispose());
        panel.add(cancelBtn);

        return panel;
    }

    private void save() {
        if (enseignantCombo.getSelectedItem() == null
            || matiereCombo.getSelectedItem() == null
            || classeCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner tous les champs.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Long idEnseignant = extractId(enseignantCombo.getSelectedItem().toString());
        Long idMatiere = extractId(matiereCombo.getSelectedItem().toString());
        Long idClasse = extractId(classeCombo.getSelectedItem().toString());

        try {
            Enseignement existing = enseignementDao.findByIds(idEnseignant, idMatiere, idClasse);
            if (existing != null) {
                JOptionPane.showMessageDialog(this, "Cette liaison existe déjà.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Enseignement ens = new Enseignement(null, idEnseignant, idMatiere, idClasse);
            enseignementDao.create(ens);
            confirmed = true;
            dispose();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur création enseignement", e);
            JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Long extractId(String comboValue) {
        try {
            return Long.parseLong(comboValue.split(" - ")[0].trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
