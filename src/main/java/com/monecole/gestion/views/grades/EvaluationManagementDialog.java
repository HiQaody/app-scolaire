package com.monecole.gestion.views.grades;

import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.dao.EvaluationDao;
import com.monecole.gestion.models.Evaluation;
import com.monecole.gestion.models.Utilisateur;
import com.monecole.gestion.services.NoteService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialogue de gestion du catalogue d'evaluations d'un enseignement.
 * <p>
 * Remplace l'onglet Evaluations : la saisie des notes devient l'ecran
 * principal et la creation d'une evaluation une tache ponctuelle, ouverte a la
 * demande depuis un seul bouton.
 */
public class EvaluationManagementDialog extends JDialog {

    private static final Logger LOGGER =
        Logger.getLogger(EvaluationManagementDialog.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] COLONNES = {
        "Type", "Libelle", "Date", "Poids", "Barème", "Notes saisies"
    };

    private final Utilisateur currentUser;
    private final NoteService noteService;
    private final EvaluationDao evaluationDao;

    private final JComboBox<ComboItem> enseignementCombo = new JComboBox<>();
    private final EvaluationTableModel model = new EvaluationTableModel();
    private final JTable table = new JTable(model);
    private final JButton modifierBtn = new JButton("Modifier");
    private final JButton supprimerBtn = new JButton("Supprimer");
    private final JButton saisirBtn = new JButton("Saisir les notes");

    private Long evaluationChoisie;
    private Long selectionApresRechargement;

    /**
     * @param owner                  fenetre parente
     * @param user                   utilisateur connecte
     * @param idEnseignementPreselect enseignement a afficher a l'ouverture, ou null
     */
    public EvaluationManagementDialog(Window owner, Utilisateur user, Long idEnseignementPreselect) {
        super(owner, "Gérer les évaluations", ModalityType.APPLICATION_MODAL);
        this.currentUser = user;
        DaoFactory df = DaoFactory.getInstance();
        this.noteService = new NoteService();
        this.evaluationDao = df.getEvaluationDao();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(760, 420));
        setLayout(new BorderLayout(10, 10));
        

        add(buildEnTete(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildBoutons(), BorderLayout.SOUTH);

        SwingUtilities.invokeLater(() -> chargerEnseignements(idEnseignementPreselect));
    }

    /** Evaluation choisie pour la saisie, ou null si l'utilisateur a juste ferme. */
    public Long getEvaluationChoisie() {
        return evaluationChoisie;
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    private JPanel buildEnTete() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.add(new JLabel("Enseignement :"), BorderLayout.WEST);

        enseignementCombo.setPreferredSize(new Dimension(360, 28));
        enseignementCombo.addActionListener(e -> onEnseignementChange());
        panel.add(enseignementCombo, BorderLayout.CENTER);

        JButton nouveauBtn = new JButton("+ Nouvelle évaluation");
        nouveauBtn.addActionListener(e -> creerEvaluation());
        panel.add(nouveauBtn, BorderLayout.EAST);
        return panel;
    }

    private JScrollPane buildTable() {
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                majBoutons();
            }
        });

        modifierBtn.addActionListener(e -> modifierEvaluation());
        supprimerBtn.addActionListener(e -> supprimerEvaluation());
        saisirBtn.addActionListener(this::choisirPourSaisie);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder(
            "Le poids et le barème pilotent la moyenne de l'évaluation"));
        return scroll;
    }

    private JPanel buildBoutons() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);

        JPanel gauche = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        gauche.setOpaque(false);
        gauche.add(modifierBtn);
        gauche.add(supprimerBtn);
        panel.add(gauche, BorderLayout.WEST);

        JPanel droite = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        droite.setOpaque(false);
        saisirBtn.setDefaultCapable(true);
        JButton fermerBtn = new JButton("Fermer");
        fermerBtn.addActionListener(e -> dispose());
        droite.add(saisirBtn);
        droite.add(fermerBtn);
        panel.add(droite, BorderLayout.EAST);

        majBoutons();
        return panel;
    }

    // ------------------------------------------------------------------
    // Chargements
    // ------------------------------------------------------------------

    private void chargerEnseignements(Long idPreselection) {
        SwingWorker<List<TeachingLabels.Teaching>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<TeachingLabels.Teaching> doInBackground() throws Exception {
                return TeachingLabels.of(currentUser);
            }

            @Override
            protected void done() {
                try {
                    List<TeachingLabels.Teaching> teachings = get();
                    enseignementCombo.removeAllItems();
                    for (TeachingLabels.Teaching t : teachings) {
                        enseignementCombo.addItem(new ComboItem(t.id(), t.libelle()));
                    }
                    if (teachings.isEmpty()) {
                        return;
                    }
                    if (idPreselection != null) {
                        selectionnerEnseignement(idPreselection);
                    } else {
                        onEnseignementChange();
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Erreur chargement enseignements", ex);
                    showError("Impossible de charger les enseignements", ex);
                }
            }
        };
        worker.execute();
    }

    private void selectionnerEnseignement(Long idEnseignement) {
        for (int i = 0; i < enseignementCombo.getItemCount(); i++) {
            ComboItem item = enseignementCombo.getItemAt(i);
            if (item != null && idEnseignement.equals(item.id())) {
                enseignementCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void onEnseignementChange() {
        ComboItem item = (ComboItem) enseignementCombo.getSelectedItem();
        if (item == null || item.id() == null) {
            model.setDonnees(List.of(), Map.of());
            majBoutons();
            return;
        }
        SwingWorker<List<Evaluation>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Evaluation> doInBackground() throws Exception {
                return evaluationDao.findByIdEnseignement(item.id());
            }

            @Override
            protected void done() {
                try {
                    List<Evaluation> evaluations = get();
                    List<Long> ids = new ArrayList<>();
                    for (Evaluation ev : evaluations) {
                        ids.add(ev.id());
                    }
                    model.setDonnees(evaluations, noteService.compterNotesParEvaluation(ids));
                    selectionner(selectionApresRechargement);
                    majBoutons();
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Erreur chargement evaluations", ex);
                    showError("Impossible de charger les évaluations", ex);
                }
            }
        };
        worker.execute();
    }

    /** Selectionne une evaluation par identifiant, ou la premiere ligne a defaut. */
    private void selectionner(Long idEvaluation) {
        if (model.getRowCount() == 0) {
            return;
        }
        if (idEvaluation != null) {
            for (int i = 0; i < model.getRowCount(); i++) {
                if (idEvaluation.equals(model.evaluationAt(i).id())) {
                    table.setRowSelectionInterval(i, i);
                    table.scrollRectToVisible(table.getCellRect(i, 0, true));
                    return;
                }
            }
        }
        table.setRowSelectionInterval(0, 0);
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void creerEvaluation() {
        Long idEnseignement = idEnseignementCourant();
        if (idEnseignement == null) {
            return;
        }
        EvaluationFormDialog form = new EvaluationFormDialog(this, idEnseignement, null);
        form.setVisible(true);
        if (form.isConfirmed()) {
            recharger();
        }
    }

    private void modifierEvaluation() {
        Evaluation evaluation = evaluationSelectionnee();
        if (evaluation == null) {
            return;
        }
        EvaluationFormDialog form =
            new EvaluationFormDialog(this, evaluation.idEnseignement(), evaluation);
        form.setVisible(true);
        if (form.isConfirmed()) {
            recharger();
        }
    }

    private void supprimerEvaluation() {
        Evaluation evaluation = evaluationSelectionnee();
        if (evaluation == null) {
            return;
        }
        int reponse = JOptionPane.showConfirmDialog(this,
            "Supprimer l'évaluation \"" + evaluation.libelle() + "\" et toutes ses notes ?",
            "Supprimer l'évaluation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (reponse != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            int supprimees = noteService.supprimerEvaluation(evaluation.id());
            recharger();
            JOptionPane.showMessageDialog(this,
                supprimees + " note(s) supprimee(s) avec l'evaluation.",
                "Suppression", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Erreur suppression evaluation", ex);
            showError("Suppression impossible", ex);
        }
    }

    private void choisirPourSaisie(ActionEvent e) {
        Evaluation evaluation = evaluationSelectionnee();
        if (evaluation != null) {
            evaluationChoisie = evaluation.id();
            dispose();
        }
    }

    /** Recharge la liste en conservant la ligne selectionnee. */
    private void recharger() {
        Evaluation precedente = evaluationSelectionnee();
        selectionApresRechargement = precedente == null ? null : precedente.id();
        onEnseignementChange();
    }

    // ------------------------------------------------------------------
    // Utilitaires
    // ------------------------------------------------------------------

    private Long idEnseignementCourant() {
        ComboItem item = (ComboItem) enseignementCombo.getSelectedItem();
        if (item == null || item.id() == null) {
            JOptionPane.showMessageDialog(this, "Sélectionnez d'abord un enseignement.",
                "Information", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return item.id();
    }

    private Evaluation evaluationSelectionnee() {
        int ligne = table.getSelectedRow();
        if (ligne < 0 || ligne >= model.getRowCount()) {
            return null;
        }
        return model.evaluationAt(table.convertRowIndexToModel(ligne));
    }

    private void majBoutons() {
        boolean ok = evaluationSelectionnee() != null;
        modifierBtn.setEnabled(ok);
        supprimerBtn.setEnabled(ok);
        saisirBtn.setEnabled(ok);
    }

    private void showError(String titre, Exception ex) {
        JOptionPane.showMessageDialog(this,
            titre + " : " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()),
            "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    private static String formatNombre(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    /** Modèle en lecture seule du catalogue d'évaluations. */
    private final class EvaluationTableModel extends AbstractTableModel {

        private List<Evaluation> evaluations = new ArrayList<>();
        private Map<Long, Integer> compteurs = Map.of();

        void setDonnees(List<Evaluation> evaluations, Map<Long, Integer> compteurs) {
            this.evaluations = new ArrayList<>(evaluations);
            this.compteurs = Map.copyOf(compteurs);
            fireTableDataChanged();
        }

        Evaluation evaluationAt(int row) {
            return evaluations.get(row);
        }

        @Override
        public int getRowCount() {
            return evaluations.size();
        }

        @Override
        public int getColumnCount() {
            return COLONNES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLONNES[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            Evaluation ev = evaluations.get(row);
            return switch (column) {
                case 0 -> ev.type().getLabel();
                case 1 -> ev.libelle();
                case 2 -> ev.date() == null ? "—" : ev.date().format(DATE_FMT);
                case 3 -> formatNombre(ev.poids());
                case 4 -> formatNombre(ev.bareme());
                case 5 -> compteurs.getOrDefault(ev.id(), 0);
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}
