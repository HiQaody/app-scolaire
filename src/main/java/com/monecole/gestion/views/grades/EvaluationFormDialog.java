package com.monecole.gestion.views.grades;

import com.monecole.gestion.models.Evaluation;
import com.monecole.gestion.services.NoteService;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Dialogue modal pour l'ajout et la modification d'une évaluation.
 * <p>
 * Le poids et le barème saisis ici conditionnent le calcul des moyennes :
 * les notes sont ramenées sur 20 via le barème, puis pondérées par le poids.
 */
public class EvaluationFormDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTextField libelleField = new JTextField(20);
    private final JComboBox<Evaluation.TypeEvaluation> typeCombo =
        new JComboBox<>(Evaluation.TypeEvaluation.values());
    private final JTextField dateField = new JTextField(12);
    private final JTextField poidsField = new JTextField(8);
    private final JTextField baremeField = new JTextField(8);
    private final JLabel apercuMoyenne = new JLabel(" ");
    private final JButton okBtn = new JButton();

    private final NoteService noteService;
    private final Long idEnseignement;
    private final Evaluation evaluation;
    private final boolean isEdit;

    private boolean confirmed;

    /**
     * @param parent        fenêtre parente
     * @param idEnseignement enseignement auquel rattacher l'évaluation
     * @param evaluation    évaluation à modifier, ou null pour une création
     */
    public EvaluationFormDialog(Window parent, Long idEnseignement, Evaluation evaluation) {
        super(parent, evaluation == null ? "Nouvelle évaluation" : "Modifier l'évaluation",
            Dialog.ModalityType.APPLICATION_MODAL);
        this.idEnseignement = idEnseignement;
        this.evaluation = evaluation;
        this.isEdit = evaluation != null;
        this.noteService = new NoteService();

        setLayout(new BorderLayout(10, 10));
        add(buildForm(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        if (isEdit) {
            populateFields(evaluation);
        } else {
            poidsField.setText("1");
            baremeField.setText("20");
            dateField.setText(LocalDate.now().format(DATE_FMT));
            typeCombo.setSelectedItem(Evaluation.TypeEvaluation.DEVOIR);
        }
        DocumentListener majApercu = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateApercu(); }
            @Override public void removeUpdate(DocumentEvent e) { updateApercu(); }
            @Override public void changedUpdate(DocumentEvent e) { updateApercu(); }
        };
        poidsField.getDocument().addDocumentListener(majApercu);
        baremeField.getDocument().addDocumentListener(majApercu);
        updateApercu();

        getRootPane().setDefaultButton(okBtn);
        pack();
        setLocationRelativeTo(parent);
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 5, 5, 5);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        int ligne = 0;
        gc.gridx = 0; gc.gridy = ligne;
        panel.add(new JLabel("Libellé :"), gc);
        gc.gridx = 1; gc.gridwidth = 2;
        panel.add(libelleField, gc);
        libelleField.setToolTipText("Ex. : Devoir 1, Examen final, Projet groupe");

        ligne++;
        gc.gridx = 0; gc.gridy = ligne;
        panel.add(new JLabel("Type :"), gc);
        gc.gridx = 1; gc.gridwidth = 1;
        panel.add(typeCombo, gc);

        ligne++;
        gc.gridx = 0; gc.gridy = ligne;
        panel.add(new JLabel("Date :"), gc);
        gc.gridx = 1;
        dateField.setToolTipText("Format jj/MM/aaaa, laisser vide si non datée");
        panel.add(dateField, gc);

        ligne++;
        gc.gridx = 0; gc.gridy = ligne;
        panel.add(new JLabel("Poids :"), gc);
        gc.gridx = 1;
        poidsField.setToolTipText("Pondération dans la moyenne de la matière (0 = non compté)");
        panel.add(poidsField, gc);

        ligne++;
        gc.gridx = 0; gc.gridy = ligne;
        panel.add(new JLabel("Barème :"), gc);
        gc.gridx = 1;
        baremeField.setToolTipText("Note maximale de l'évaluation, généralement 20");
        panel.add(baremeField, gc);

        ligne++;
        gc.gridx = 0; gc.gridy = ligne;
        gc.gridwidth = 3;
        JPanel aides = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        aides.setOpaque(false);
        ButtonGroup groupe = new ButtonGroup();
        for (double barème : new double[]{10, 20, 40, 100}) {
            JRadioButton bouton = new JRadioButton(String.valueOf((int) barème));
            bouton.setFocusPainted(false);
            if ((int) barème == 20) {
                bouton.setSelected(true);
            }
            groupe.add(bouton);
            bouton.addActionListener(e -> baremeField.setText(String.valueOf((int) barème)));
            aides.add(bouton);
        }
        panel.add(aides, gc);

        ligne++;
        gc.gridx = 0; gc.gridy = ligne;
        gc.gridwidth = 3;
        apercuMoyenne.setFont(apercuMoyenne.getFont().deriveFont(Font.ITALIC, 12f));
        panel.add(apercuMoyenne, gc);

        return panel;
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        okBtn.setText(isEdit ? "Mettre à jour" : "Créer l'évaluation");
        okBtn.setFocusPainted(false);
        okBtn.addActionListener(e -> save());
        panel.add(okBtn);

        JButton cancelBtn = new JButton("Annuler");
        cancelBtn.setFocusPainted(false);
        cancelBtn.addActionListener(e -> dispose());
        panel.add(cancelBtn);

        return panel;
    }

    private void populateFields(Evaluation ev) {
        libelleField.setText(ev.libelle());
        typeCombo.setSelectedItem(ev.type());
        dateField.setText(ev.date() == null ? "" : ev.date().format(DATE_FMT));
        poidsField.setText(trimDecimal(ev.poids()));
        baremeField.setText(trimDecimal(ev.bareme()));
    }

    /** Aperçu de l'effet du barème sur une note, pour éviter les erreurs de saisie. */
    private void updateApercu() {
        Double bareme = parseDouble(baremeField.getText());
        Double poids = parseDouble(poidsField.getText());
        if (bareme == null || bareme <= 0) {
            apercuMoyenne.setText("Barème invalide.");
            return;
        }
        StringBuilder sb = new StringBuilder("Sur ce barème, ");
        sb.append(String.format("%.2f", bareme)).append(" = 20/20");
        if (poids != null) {
            sb.append(String.format("  •  poids %s dans la moyenne de la matière", trimDecimal(poids)));
        }
        apercuMoyenne.setText(sb.toString());
    }

    private void save() {
        String libelle = libelleField.getText().trim();
        Evaluation.TypeEvaluation type = (Evaluation.TypeEvaluation) typeCombo.getSelectedItem();
        LocalDate date = parseDate(dateField.getText());
        Double poids = parseDouble(poidsField.getText());
        Double bareme = parseDouble(baremeField.getText());

        if (dateField.getText().trim().isEmpty()) {
            date = null;
        } else if (date == null) {
            JOptionPane.showMessageDialog(this,
                "Date invalide : utilisez le format jj/MM/aaaa.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (isEdit) {
                noteService.modifierEvaluation(evaluation, libelle, type, date,
                    poids == null ? 1 : poids, bareme == null ? 20 : bareme);
            } else {
                noteService.creerEvaluation(idEnseignement, libelle, type, date,
                    poids == null ? 1 : poids, bareme == null ? 20 : bareme);
            }
            confirmed = true;
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static Double parseDouble(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), DATE_FMT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String trimDecimal(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    /** Vrai si l'utilisateur a validé le formulaire. */
    public boolean isConfirmed() {
        return confirmed;
    }
}
