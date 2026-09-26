package com.monecole.gestion.views.grades;

import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.dao.EvaluationDao;
import com.monecole.gestion.models.Evaluation;
import com.monecole.gestion.models.Etudiant;
import com.monecole.gestion.models.Utilisateur;
import com.monecole.gestion.services.NoteService;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Vue de saisie des notes et de synthèse de classe.
 * <p>
 * Interface volontairement réduite à l'essentiel : une seule liste déroulante
 * choisit l'évaluation, les notes s'enregistrent automatiquement à la sortie
 * d'une cellule, et les actions rares sont regroupées dans un menu « Plus ».
 * Les statistiques sont résumées sur une ligne et l'histogramme reste replié
 * tant qu'il n'est pas demandé.
 * <p>
 * Toutes les requêtes SQL sont exécutées hors du thread EDT via
 * {@link SwingWorker}, et un jeton de génération invalide les résultats
 * obsolètes lorsqu'on change d'évaluation pendant un chargement.
 */
public class NoteManagementView extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(NoteManagementView.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int NOTE_COLUMN = 1;
    private static final int ONGLET_SAISIE = 0;
    private static final int ONGLET_SYNTHESE = 1;
    /** Délai de regroupement des saisies rapides avant écriture en base. */
    private static final int DELAI_ENREGISTREMENT_MS = 600;

    private static final String CARTE_MESSAGE = "message";
    private static final String CARTE_SYNTHESE = "synthese";

    private static final Color COULEUR_INFO = new Color(0x5F, 0x6B, 0x7A);
    private static final Color COULEUR_OK = new Color(0x1E, 0x88, 0x3E);
    private static final Color COULEUR_ERREUR = new Color(0xC0, 0x39, 0x2B);

    private final Utilisateur currentUser;
    private final NoteService noteService;
    private final EvaluationDao evaluationDao;

    private final JComboBox<ComboItem> evaluationCombo = new JComboBox<>();
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel resumeLabel = new JLabel(" ");
    private final List<Long> rowEtudiantIds = new ArrayList<>();
    /** Notes telles qu'enregistrées en base, pour ne persister que le différentiel. */
    private final Map<Long, Double> savedNotes = new LinkedHashMap<>();
    private final AtomicInteger loadGeneration = new AtomicInteger();
    private final Timer saveTimer;

    private JTable noteTable;
    private DefaultTableModel noteTableModel;
    private StatsPanel statsPanel;
    private SynthesePanel synthesePanel;
    private JTabbedPane tabs;
    private JPanel contenuSynthese;
    private CardLayout syntheseLayout;
    private JLabel messageSynthese;
    private JButton histogrammeBtn;
    private CardLayout detailLayout;
    private JPanel detailPanel;
    private JMenuItem remplirItem;
    private JMenuItem copierItem;
    private JMenuItem effacerItem;
    private JCheckBoxMenuItem lockItem;

    private List<Etudiant> currentEtudiants = List.of();
    private Long currentClasseId;
    private Evaluation currentEvaluation;
    private double currentBareme = 20.0;

    private boolean isLocked;
    private boolean isBusy;
    private boolean suppressAutoSave;

    public NoteManagementView(Utilisateur currentUser) {
        this.currentUser = currentUser;
        DaoFactory df = DaoFactory.getInstance();
        this.noteService = new NoteService();
        this.evaluationDao = df.getEvaluationDao();
        this.saveTimer = new Timer(DELAI_ENREGISTREMENT_MS, e -> enregistrerDiff());
        this.saveTimer.setRepeats(false);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> chargerEvaluations(null));
    }

    // ------------------------------------------------------------------
    // Construction de l'interface
    // ------------------------------------------------------------------

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);

        JLabel titre = new JLabel("Gestion des notes");
        titre.setFont(new Font("Segoe UI", Font.BOLD, 18));
        panel.add(titre, BorderLayout.WEST);

        JPanel centre = new JPanel(new BorderLayout(8, 0));
        centre.setOpaque(false);
        evaluationCombo.setPreferredSize(new Dimension(480, 30));
        evaluationCombo.setToolTipText("Évaluation à saisir : la plus récente est proposée");
        evaluationCombo.addActionListener(e -> onEvaluationChanged());
        centre.add(evaluationCombo, BorderLayout.CENTER);

        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        statusLabel.setForeground(COULEUR_INFO);
        centre.add(statusLabel, BorderLayout.EAST);
        panel.add(centre, BorderLayout.CENTER);

        JButton gererBtn = new JButton("Gérer les évaluations…");
        gererBtn.setFocusPainted(false);
        gererBtn.setToolTipText("Créer, modifier ou supprimer une évaluation");
        gererBtn.addActionListener(e -> ouvrirGestionEvaluations());
        panel.add(gererBtn, BorderLayout.EAST);
        return panel;
    }

    private JTabbedPane buildTabs() {
        tabs = new JTabbedPane();
        tabs.addTab("Saisie", buildSaisieTab());

        syntheseLayout = new CardLayout();
        contenuSynthese = new JPanel(syntheseLayout);
        messageSynthese = new JLabel(" ", SwingConstants.CENTER);
        messageSynthese.setForeground(COULEUR_INFO);
        synthesePanel = new SynthesePanel();
        contenuSynthese.add(messageSynthese, CARTE_MESSAGE);
        contenuSynthese.add(synthesePanel, CARTE_SYNTHESE);
        tabs.addTab("Synthèse", contenuSynthese);

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == ONGLET_SYNTHESE) {
                chargerSynthese();
            }
        });
        return tabs;
    }

    private JPanel buildSaisieTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);

        resumeLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(buildBarreResume(), BorderLayout.NORTH);

        noteTableModel = new DefaultTableModel(new Object[]{"Étudiant", "Note"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == NOTE_COLUMN ? Double.class : String.class;
            }
        };
        noteTableModel.addTableModelListener(e -> {
            if (suppressAutoSave || e.getColumn() != NOTE_COLUMN) {
                return;
            }
            majResume();
            planifierEnregistrement();
        });

        noteTable = new JTable(noteTableModel) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return !isLocked && !isBusy && col == NOTE_COLUMN;
            }

            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                return column == NOTE_COLUMN ? noteRenderer : super.getCellRenderer(row, column);
            }

            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                return column == NOTE_COLUMN ? noteEditor : super.getCellEditor(row, column);
            }
        };
        noteTable.setRowHeight(26);
        noteTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        noteTable.setFillsViewportHeight(true);
        noteTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        noteTable.getColumnModel().getColumn(0).setPreferredWidth(360);
        noteTable.getColumnModel().getColumn(NOTE_COLUMN).setPreferredWidth(110);
        installKeyboardNavigation(noteTable);

        JScrollPane scroll = new JScrollPane(noteTable);
        scroll.setBorder(BorderFactory.createTitledBorder(
            "Une note par étudiant — enregistrée automatiquement"));
        panel.add(scroll, BorderLayout.CENTER);

        statsPanel = new StatsPanel();
        detailLayout = new CardLayout();
        detailPanel = new JPanel(detailLayout);
        detailPanel.add(statsPanel, "detail");
        detailPanel.add(new JPanel(), "replie");
        detailLayout.show(detailPanel, "replie");
        panel.add(detailPanel, BorderLayout.SOUTH);
        return panel;
    }

    /** Résumé chiffré en une ligne, seul moyen d'accéder au détail et aux actions. */
    private JPanel buildBarreResume() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.add(resumeLabel, BorderLayout.CENTER);

        JPanel droite = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        droite.setOpaque(false);

        histogrammeBtn = new JButton("▸ Détails");
        histogrammeBtn.setFocusPainted(false);
        histogrammeBtn.setToolTipText("Afficher la médiane, l'écart-type et l'histogramme");
        histogrammeBtn.addActionListener(e -> basculerDetails());
        droite.add(histogrammeBtn);

        JButton actionsBtn = new JButton("⋯ Plus");
        actionsBtn.setFocusPainted(false);
        JPopupMenu menu = buildMenuActions();
        actionsBtn.addActionListener(e -> menu.show(actionsBtn, 0, actionsBtn.getHeight()));
        droite.add(actionsBtn);

        panel.add(droite, BorderLayout.EAST);
        return panel;
    }

    private JPopupMenu buildMenuActions() {
        JPopupMenu menu = new JPopupMenu();

        remplirItem = new JMenuItem("Remplir toute la classe…");
        remplirItem.addActionListener(e -> remplirClasse());
        menu.add(remplirItem);

        copierItem = new JMenuItem("Copier l'évaluation précédente");
        copierItem.addActionListener(e -> copierEvaluationPrecedente());
        menu.add(copierItem);

        effacerItem = new JMenuItem("Tout effacer les notes");
        effacerItem.addActionListener(e -> toutEffacer());
        menu.add(effacerItem);

        menu.addSeparator();
        lockItem = new JCheckBoxMenuItem("Verrouiller la saisie", false);
        lockItem.setToolTipText("Empêche toute modification des notes de l'évaluation courante");
        lockItem.addActionListener(e -> setLocked(lockItem.isSelected()));
        menu.add(lockItem);

        return menu;
    }

    // ------------------------------------------------------------------
    // Chargements
    // ------------------------------------------------------------------

    /**
     * Charge toutes les évaluations accessibles à l'utilisateur dans une liste
     * unique, libellées par matière et classe.
     *
     * @param idASelectionner évaluation à présélectionner, ou null pour la plus récente
     */
    private void chargerEvaluations(Long idASelectionner) {
        int generation = loadGeneration.incrementAndGet();
        setBusy(true);
        setStatus("Chargement…", COULEUR_INFO);

        SwingWorker<List<ComboItem>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ComboItem> doInBackground() throws Exception {
                Map<Long, TeachingLabels.Teaching> parId =
                    TeachingLabels.indexParId(TeachingLabels.of(currentUser));
                List<Evaluation> evaluations = new ArrayList<>(fetchEvaluations());
                evaluations.sort(Comparator
                    .comparing(Evaluation::date, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Evaluation::id, Comparator.reverseOrder()));

                List<ComboItem> items = new ArrayList<>();
                for (Evaluation ev : evaluations) {
                    TeachingLabels.Teaching t = parId.get(ev.idEnseignement());
                    items.add(ComboItem.pourEvaluation(ev.id(),
                        t == null ? "?" : t.matiere(),
                        t == null ? "?" : t.classe(),
                        ev.type().getLabel(), ev.libelle(),
                        ev.date() == null ? "—" : ev.date().format(DATE_FMT)));
                }
                return items;
            }

            @Override
            protected void done() {
                if (generation != loadGeneration.get()) {
                    return;
                }
                try {
                    List<ComboItem> items = get();
                    evaluationCombo.removeAllItems();
                    for (ComboItem item : items) {
                        evaluationCombo.addItem(item);
                    }
                    setBusy(false);
                    if (items.isEmpty()) {
                        setStatus("Aucune évaluation.", COULEUR_INFO);
                        afficherMessageSynthese("Aucune évaluation à saisir pour le moment.");
                        return;
                    }
                    evaluationCombo.setSelectedIndex(
                        indexOf(idASelectionner) >= 0 ? indexOf(idASelectionner) : 0);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Erreur chargement évaluations", ex);
                    setBusy(false);
                    setStatus("Erreur de chargement.", COULEUR_ERREUR);
                    showError("Impossible de charger les évaluations", ex);
                }
            }
        };
        worker.execute();
    }

    /** Position d'une évaluation dans la liste, ou -1 si elle a disparu. */
    private int indexOf(Long idEvaluation) {
        if (idEvaluation == null) {
            return -1;
        }
        for (int i = 0; i < evaluationCombo.getItemCount(); i++) {
            ComboItem item = evaluationCombo.getItemAt(i);
            if (item != null && idEvaluation.equals(item.id())) {
                return i;
            }
        }
        return -1;
    }

    /** Un enseignant ne voit que ses évaluations, les autres rôles toutes. */
    private List<Evaluation> fetchEvaluations() throws Exception {
        if (currentUser != null
            && currentUser.role() == Utilisateur.Role.ENSEIGNANT
            && currentUser.idEnseignant() != null) {
            return evaluationDao.findByIdEnseignant(currentUser.idEnseignant());
        }
        return evaluationDao.findAll();
    }

    private void onEvaluationChanged() {
        ComboItem item = (ComboItem) evaluationCombo.getSelectedItem();
        viderGrille();
        if (item == null || item.id() == null) {
            setStatus("Choisissez une évaluation.", COULEUR_INFO);
            return;
        }

        Long idEvaluation = item.id();
        int generation = loadGeneration.incrementAndGet();
        setBusy(true);
        setStatus("Chargement…", COULEUR_INFO);

        SwingWorker<NoteService.GrilleSaisie, Void> worker = new SwingWorker<>() {
            @Override
            protected NoteService.GrilleSaisie doInBackground() throws Exception {
                return noteService.chargerGrille(idEvaluation);
            }

            @Override
            protected void done() {
                if (generation != loadGeneration.get()) {
                    return;
                }
                try {
                    afficherGrille(get());
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Erreur chargement grille", ex);
                    setBusy(false);
                    setStatus("Erreur de chargement.", COULEUR_ERREUR);
                    showError("Impossible de charger les étudiants", ex);
                }
            }
        };
        worker.execute();
    }

    private void afficherGrille(NoteService.GrilleSaisie grille) {
        currentEvaluation = grille.evaluation();
        currentClasseId = grille.idClasse();
        currentBareme = currentEvaluation.bareme();
        currentEtudiants = grille.etudiants();

        rowEtudiantIds.clear();
        savedNotes.clear();
        savedNotes.putAll(grille.notes());
        setLocked(false);
        noteTable.getColumnModel().getColumn(NOTE_COLUMN)
            .setHeaderValue("Note / " + formatNombre(currentBareme));

        suppressAutoSave = true;
        try {
            noteTableModel.setRowCount(0);
            for (Etudiant e : currentEtudiants) {
                rowEtudiantIds.add(e.id());
                noteTableModel.addRow(new Object[]{
                    e.nom() + " " + e.prenom() + " (" + e.matricule() + ")",
                    grille.notes().get(e.id())
                });
            }
        } finally {
            suppressAutoSave = false;
        }

        setBusy(false);
        majResume();
        statsPanel.setStatistiques(grille.statistiques());
        setStatus(currentEtudiants.size() + " étudiant(s) · notes enregistrées au fil de la saisie",
            COULEUR_INFO);
        if (currentEtudiants.isEmpty()) {
            setStatus("Aucun étudiant dans cette classe.", COULEUR_INFO);
        }
    }

    // ------------------------------------------------------------------
    // Synthèse
    // ------------------------------------------------------------------

    private void chargerSynthese() {
        Long classeId = currentClasseId;
        if (classeId == null) {
            afficherMessageSynthese("Choisissez une évaluation pour afficher "
                + "la synthèse de sa classe.");
            return;
        }
        SwingWorker<SynthesePanel.Charge, Void> worker = new SwingWorker<>() {
            @Override
            protected SynthesePanel.Charge doInBackground() throws Exception {
                return SynthesePanel.preparer(noteService.syntheseClasse(classeId));
            }

            @Override
            protected void done() {
                try {
                    synthesePanel.appliquer(get());
                    syntheseLayout.show(contenuSynthese, CARTE_SYNTHESE);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Erreur synthèse", ex);
                    afficherMessageSynthese("La synthèse n'a pas pu être calculée.");
                    showError("Erreur lors du calcul de la synthèse", ex);
                }
            }
        };
        worker.execute();
    }

    private void afficherMessageSynthese(String message) {
        messageSynthese.setText(message);
        syntheseLayout.show(contenuSynthese, CARTE_MESSAGE);
    }

    // ------------------------------------------------------------------
    // Résumé et enregistrement automatique
    // ------------------------------------------------------------------

    /** Statistiques recalculées depuis le tableau, sans accès base. */
    private void majResume() {
        Map<Long, Double> valeurs = collectValeurs();
        NoteService.StatsEvaluation stats = noteService.statistiques(currentEtudiants, valeurs, currentBareme);
        statsPanel.setStatistiques(stats);

        if (stats.effectif() == 0) {
            resumeLabel.setText(" ");
            return;
        }
        if (stats.moyenne() == null) {
            resumeLabel.setText("Aucune note saisie pour cette évaluation.");
            return;
        }
        resumeLabel.setText(String.format(
            "Moyenne %.2f / %s   ·   Min %s   ·   Max %s   ·   Réussite %s   ·   %d non noté(s)",
            stats.moyenne(), formatNombre(stats.bareme()),
            formatNombre(stats.minimum()), formatNombre(stats.maximum()),
            stats.tauxReussitePourcentage(), stats.nbSansNote()));
    }

    private void basculerDetails() {
        boolean visibles = histogrammeBtn.getText().startsWith("▾");
        if (visibles) {
            detailLayout.show(detailPanel, "replie");
            histogrammeBtn.setText("▸ Détails");
        } else {
            detailLayout.show(detailPanel, "detail");
            histogrammeBtn.setText("▾ Détails");
        }
    }

    private void planifierEnregistrement() {
        if (currentEvaluation == null || isBusy) {
            return;
        }
        setStatus("Enregistrement…", COULEUR_INFO);
        saveTimer.restart();
    }

    /** N'écrit que le différentiel entre le tableau et le dernier état connu. */
    private void enregistrerDiff() {
        if (currentEvaluation == null || currentEtudiants.isEmpty()) {
            return;
        }
        Map<Long, Double> valeurs = collectValeurs();

        Map<Long, Double> modifies = new LinkedHashMap<>();
        for (Map.Entry<Long, Double> entry : valeurs.entrySet()) {
            Double connu = savedNotes.get(entry.getKey());
            if (connu == null || connu.doubleValue() != entry.getValue().doubleValue()) {
                modifies.put(entry.getKey(), entry.getValue());
            }
        }
        List<Long> retires = new ArrayList<>();
        for (Long id : savedNotes.keySet()) {
            if (!valeurs.containsKey(id)) {
                retires.add(id);
            }
        }
        if (modifies.isEmpty() && retires.isEmpty()) {
            setStatus("Enregistré", COULEUR_OK);
            return;
        }

        Long idEvaluation = currentEvaluation.id();
        int generation = loadGeneration.get();
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (!modifies.isEmpty()) {
                    noteService.enregistrer(idEvaluation, modifies);
                }
                if (!retires.isEmpty()) {
                    noteService.effacerNotes(idEvaluation, retires);
                }
                return null;
            }

            @Override
            protected void done() {
                if (generation != loadGeneration.get()) {
                    return;
                }
                try {
                    get();
                    savedNotes.clear();
                    savedNotes.putAll(valeurs);
                    setStatus("Enregistré", COULEUR_OK);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Erreur enregistrement notes", ex);
                    setStatus("Échec de l'enregistrement", COULEUR_ERREUR);
                    showError("Erreur lors de l'enregistrement des notes", ex);
                    rechargerGrille();
                }
            }
        };
        worker.execute();
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void ouvrirGestionEvaluations() {
        saveTimer.stop();
        Long idEnseignement = currentEvaluation == null ? null : currentEvaluation.idEnseignement();
        EvaluationManagementDialog dialog =
            new EvaluationManagementDialog(SwingUtilities.getWindowAncestor(this), currentUser, idEnseignement);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        Long choisie = dialog.getEvaluationChoisie();
        chargerEvaluations(choisie);
    }

    private void copierEvaluationPrecedente() {
        if (currentEvaluation == null) {
            return;
        }
        try {
            var precedente = noteService.evaluationPrecedente(currentEvaluation);
            if (precedente.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Aucune évaluation antérieure pour cet enseignement.",
                    "Information", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Evaluation source = precedente.get();
            int reponse = JOptionPane.showConfirmDialog(this,
                "Remplacer la saisie par les notes de \"" + source.libelle() + "\" ("
                    + source.type().getLabel() + ") ?\nLes notes actuelles seront perdues.",
                "Copier une évaluation", JOptionPane.OK_CANCEL_OPTION);
            if (reponse != JOptionPane.OK_OPTION) {
                return;
            }
            int copiees = noteService.copierDepuis(source.id(), currentEvaluation, currentClasseId);
            rechargerGrille();
            JOptionPane.showMessageDialog(this, copiees + " note(s) copiée(s) et ramenée(s) sur "
                    + formatNombre(currentBareme) + ".",
                "Succès", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Erreur copie évaluation", ex);
            showError("Erreur lors de la copie", ex);
        }
    }

    private void remplirClasse() {
        if (currentEvaluation == null) {
            return;
        }
        String saisie = JOptionPane.showInputDialog(this,
            "Note à appliquer à toute la classe (entre 0 et " + formatNombre(currentBareme) + ") :",
            "Remplir la classe", JOptionPane.QUESTION_MESSAGE);
        if (saisie == null) {
            return;
        }
        double valeur;
        try {
            valeur = Double.parseDouble(saisie.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this, "Saisissez un nombre valide.",
                "Valeur invalide", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (valeur < 0 || valeur > currentBareme) {
            JOptionPane.showMessageDialog(this,
                "La note doit être comprise entre 0 et " + formatNombre(currentBareme) + ".",
                "Valeur invalide", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int nb = noteService.remplirClasse(currentEvaluation.id(), currentClasseId, valeur);
            rechargerGrille();
            JOptionPane.showMessageDialog(this, nb + " note(s) enregistrée(s).",
                "Succès", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Erreur remplissage classe", ex);
            showError("Erreur lors du remplissage", ex);
        }
    }

    private void toutEffacer() {
        if (currentEvaluation == null) {
            return;
        }
        int reponse = JOptionPane.showConfirmDialog(this,
            "Supprimer toutes les notes de \"" + currentEvaluation.libelle() + "\" ?",
            "Tout effacer", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (reponse != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            noteService.toutEffacer(currentEvaluation.id(), currentClasseId);
            rechargerGrille();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Erreur effacement", ex);
            showError("Erreur lors de l'effacement", ex);
        }
    }

    private void rechargerGrille() {
        if (currentEvaluation == null) {
            return;
        }
        Long idEvaluation = currentEvaluation.id();
        int generation = loadGeneration.incrementAndGet();
        setBusy(true);
        setStatus("Rechargement…", COULEUR_INFO);

        SwingWorker<NoteService.GrilleSaisie, Void> worker = new SwingWorker<>() {
            @Override
            protected NoteService.GrilleSaisie doInBackground() throws Exception {
                return noteService.chargerGrille(idEvaluation);
            }

            @Override
            protected void done() {
                if (generation != loadGeneration.get()) {
                    return;
                }
                try {
                    afficherGrille(get());
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Erreur rechargement grille", ex);
                    setBusy(false);
                    showError("Erreur lors du rechargement", ex);
                }
            }
        };
        worker.execute();
    }

    // ------------------------------------------------------------------
    // Utilitaires
    // ------------------------------------------------------------------

    private Map<Long, Double> collectValeurs() {
        Map<Long, Double> valeurs = new LinkedHashMap<>();
        for (int i = 0; i < noteTableModel.getRowCount() && i < rowEtudiantIds.size(); i++) {
            if (noteTableModel.getValueAt(i, NOTE_COLUMN) instanceof Double d) {
                valeurs.put(rowEtudiantIds.get(i), d);
            }
        }
        return valeurs;
    }

    /** Entrée valide et passe à la ligne suivante, Suppr efface la sélection. */
    private void installKeyboardNavigation(JTable table) {
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    int row = table.getSelectedRow();
                    if (row < 0) {
                        return;
                    }
                    if (table.isEditing()) {
                        table.getCellEditor().stopCellEditing();
                    }
                    if (row + 1 < table.getRowCount()) {
                        table.setRowSelectionInterval(row + 1, row + 1);
                        table.scrollRectToVisible(table.getCellRect(row + 1, NOTE_COLUMN, true));
                        table.editCellAt(row + 1, NOTE_COLUMN);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    e.consume();
                    effacerLignesSelectionnees();
                }
            }
        });
    }

    private void effacerLignesSelectionnees() {
        if (isLocked || isBusy) {
            return;
        }
        int[] lignes = noteTable.getSelectedRows();
        if (lignes.length == 0) {
            return;
        }
        for (int ligne : lignes) {
            if (ligne < noteTableModel.getRowCount()) {
                noteTableModel.setValueAt(null, ligne, NOTE_COLUMN);
            }
        }
    }

    private void setLocked(boolean locked) {
        isLocked = locked;
        if (lockItem != null) {
            lockItem.setSelected(locked);
        }
        for (int i = 0; i < noteTableModel.getRowCount(); i++) {
            noteTableModel.fireTableCellUpdated(i, NOTE_COLUMN);
        }
        if (locked) {
            setStatus("Saisie verrouillée", COULEUR_INFO);
        } else if (currentEvaluation != null && !currentEtudiants.isEmpty()) {
            setStatus("Saisie déverrouillée", COULEUR_INFO);
        }
    }

    private void viderGrille() {
        saveTimer.stop();
        suppressAutoSave = true;
        try {
            noteTableModel.setRowCount(0);
        } finally {
            suppressAutoSave = false;
        }
        rowEtudiantIds.clear();
        savedNotes.clear();
        currentEtudiants = List.of();
        currentEvaluation = null;
        currentClasseId = null;
        currentBareme = 20.0;
        setLocked(false);
        statsPanel.setStatistiques(null);
        resumeLabel.setText(" ");
        majEtatActions();
        afficherMessageSynthese("Choisissez une évaluation pour afficher la synthèse de sa classe.");
    }

    private void setBusy(boolean busy) {
        isBusy = busy;
        if (noteTable != null) {
            noteTable.setEnabled(!busy);
        }
        majEtatActions();
    }

    private void majEtatActions() {
        boolean active = currentEvaluation != null && !isBusy && !currentEtudiants.isEmpty();
        if (remplirItem != null) {
            remplirItem.setEnabled(active);
            copierItem.setEnabled(active);
            effacerItem.setEnabled(active);
        }
    }

    private void setStatus(String texte, Color couleur) {
        statusLabel.setText(texte);
        statusLabel.setForeground(couleur);
    }

    private void showError(String titre, Exception ex) {
        JOptionPane.showMessageDialog(this,
            titre + " : " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()),
            "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    private static String formatNombre(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    // ------------------------------------------------------------------
    // Renderer et éditeur
    // ------------------------------------------------------------------

    private final NoteCellRenderer noteRenderer = new NoteCellRenderer();
    private final NoteCellEditor noteEditor = new NoteCellEditor();

    /** Renderer de la colonne Note : couleur selon la note sur 20, aspect verrouillé. */
    private final class NoteCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(CENTER);
            if (!isSelected) {
                setForeground(table.getForeground());
                setBackground(isLocked ? new Color(0xE8, 0xE8, 0xE8)
                    : (row % 2 == 0 ? table.getBackground() : new Color(0xF5, 0xF6, 0xFA)));
            }
            if (value instanceof Double d) {
                double sur20 = currentBareme <= 0 ? d : d * 20.0 / currentBareme;
                setText(formatNombre(d)
                    + (currentBareme == 20.0 ? "" : " / " + formatNombre(currentBareme)));
                if (!isSelected) {
                    setForeground(sur20 < NoteService.SEUIL_REUSSITE
                        ? COULEUR_ERREUR : table.getForeground());
                }
            } else {
                setText("—");
            }
            return this;
        }
    }

    /**
     * Editor de la colonne Note : accepte la virgule décimale, refuse une saisie
     * invalide au lieu d'écraser silencieusement, et valide la plage 0..barème.
     */
    private final class NoteCellEditor extends DefaultCellEditor {
        private final JTextField field;

        NoteCellEditor() {
            super(new JTextField());
            field = (JTextField) editorComponent;
            field.setHorizontalAlignment(SwingConstants.CENTER);
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    field.selectAll();
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            field.setText(value instanceof Double d ? formatNombre(d) : "");
            return field;
        }

        @Override
        public boolean stopCellEditing() {
            String text = field.getText().trim().replace(',', '.');
            if (text.isEmpty()) {
                return super.stopCellEditing();
            }
            double parsed;
            try {
                parsed = Double.parseDouble(text);
            } catch (NumberFormatException e) {
                Toolkit.getDefaultToolkit().beep();
                field.selectAll();
                return false;
            }
            if (parsed < 0 || parsed > currentBareme) {
                JOptionPane.showMessageDialog(field,
                    "La note doit être comprise entre 0 et " + formatNombre(currentBareme) + ".",
                    "Note invalide", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            return super.stopCellEditing();
        }

        @Override
        public Object getCellEditorValue() {
            String text = field.getText().trim().replace(',', '.');
            if (text.isEmpty()) {
                return null;
            }
            try {
                return Double.valueOf(text);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
