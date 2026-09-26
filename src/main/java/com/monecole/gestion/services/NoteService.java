package com.monecole.gestion.services;

import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.dao.EvaluationDao;
import com.monecole.gestion.dao.NoteDao;
import com.monecole.gestion.models.Evaluation;
import com.monecole.gestion.models.Etudiant;
import com.monecole.gestion.models.Note;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service de gestion des notes : saisie par évaluation, statistiques de classe
 * et opérations sur les évaluations.
 * <p>
 * Centralise la logique métier pour que les vues n'aient qu'à gérer l'affichage.
 * Le cache de {@link MoyenneService} est invalidé après chaque écriture.
 */
public class NoteService {

    private static final Logger LOGGER = Logger.getLogger(NoteService.class.getName());

    /** Nombre de tranches de l'histogramme, sur une échelle 0-20. */
    public static final int TRANCHES = 10;
    /** Note de réussite par défaut. */
    public static final double SEUIL_REUSSITE = 10.0;

    private final NoteDao noteDao;
    private final EvaluationDao evaluationDao;
    private final MoyenneService moyenneService;

    public NoteService() {
        DaoFactory df = DaoFactory.getInstance();
        this.noteDao = df.getNoteDao();
        this.evaluationDao = df.getEvaluationDao();
        this.moyenneService = new MoyenneService();
    }

    // ------------------------------------------------------------------
    // Saisie
    // ------------------------------------------------------------------

    /**
     * Charge tout ce nécessaire à la grille de saisie d'une évaluation :
     * étudiants de la classe, notes déjà enregistrées et statistiques.
     * Réalise trois requêtes au total, quelle que soit la taille de la classe.
     */
    public GrilleSaisie chargerGrille(Long idEvaluation) throws Exception {
        Evaluation evaluation = evaluationDao.findById(idEvaluation);
        if (evaluation == null) {
            throw new IllegalArgumentException("Évaluation introuvable : " + idEvaluation);
        }
        var enseignement = DaoFactory.getInstance().getEnseignementDao().findById(evaluation.idEnseignement());
        if (enseignement == null) {
            throw new IllegalStateException("Enseignement introuvable pour l'évaluation " + idEvaluation);
        }
        Long idClasse = enseignement.idClasse();

        List<Etudiant> etudiants = DaoFactory.getInstance().getEtudiantDao().findByIdClasse(idClasse);
        Map<Long, Double> notes = new LinkedHashMap<>();
        for (Note note : noteDao.findByIdEvaluation(idEvaluation)) {
            if (note.valeur() != null) {
                notes.put(note.idEtudiant(), note.valeur());
            }
        }
        return new GrilleSaisie(evaluation, idClasse, etudiants, notes,
            statistiques(etudiants, notes, evaluation.bareme()));
    }

    /**
     * Enregistre les notes saisies et invalide le cache des moyennes.
     * Les cellules laissées vides sont ignorées, les notes déjà présentes sont mises à jour.
     *
     * @return nombre de notes écrites
     */
    public int enregistrer(Long idEvaluation, Map<Long, Double> valeurs) throws Exception {
        int ecrites = noteDao.saveNotesForEvaluation(idEvaluation, new LinkedHashMap<>(valeurs));
        invaliderCaches();
        return ecrites;
    }

    /**
     * Supprime les notes des étudiants indiqués pour une évaluation.
     * Sert à effacer une saisie ou à retirer une note devenu vide.
     */
    public int effacerNotes(Long idEvaluation, List<Long> idsEtudiants) throws Exception {
        int supprimees = noteDao.deleteForEvaluationAndEtudiants(idEvaluation, idsEtudiants);
        invaliderCaches();
        return supprimees;
    }

    /** Efface toutes les notes d'une évaluation pour une classe. */
    public int toutEffacer(Long idEvaluation, Long idClasse) throws Exception {
        List<Etudiant> etudiants = DaoFactory.getInstance().getEtudiantDao().findByIdClasse(idClasse);
        List<Long> ids = new ArrayList<>(etudiants.size());
        for (Etudiant e : etudiants) {
            ids.add(e.id());
        }
        return effacerNotes(idEvaluation, ids);
    }

    /**
     * Applique une note unique à toute la classe, pratique pour une participation
     * ou une absence justifiée notée 0.
     */
    public int remplirClasse(Long idEvaluation, Long idClasse, double valeur) throws Exception {
        List<Etudiant> etudiants = DaoFactory.getInstance().getEtudiantDao().findByIdClasse(idClasse);
        Map<Long, Double> valeurs = new LinkedHashMap<>();
        for (Etudiant e : etudiants) {
            valeurs.put(e.id(), valeur);
        }
        return enregistrer(idEvaluation, valeurs);
    }

    /**
     * Recopie les notes d'une autre évaluation vers celle-ci.
     * Les notes sont ramenées sur le barème cible si les barèmes diffèrent.
     *
     * @return nombre de notes recopiées
     */
    public int copierDepuis(Long idEvaluationSource, Evaluation cible, Long idClasse) throws Exception {
        Evaluation source = evaluationDao.findById(idEvaluationSource);
        if (source == null) {
            throw new IllegalArgumentException("Évaluation source introuvable : " + idEvaluationSource);
        }
        List<Etudiant> etudiants = DaoFactory.getInstance().getEtudiantDao().findByIdClasse(idClasse);
        Map<Long, Double> valeurs = new LinkedHashMap<>();
        for (Note note : noteDao.findByIdEvaluation(idEvaluationSource)) {
            if (note.valeur() == null) {
                continue;
            }
            valeurs.put(note.idEtudiant(), source.noteSur20(note.valeur()) * cible.bareme() / 20.0);
        }
        return enregistrer(cible.id(), valeurs);
    }

    /**
     * Évaluation précédente d'un même enseignement (ordre date puis id),
     * servant de source au bouton « copier depuis l'évaluation précédente ».
     */
    public Optional<Evaluation> evaluationPrecedente(Evaluation evaluation) throws Exception {
        if (evaluation == null || evaluation.id() == null) {
            return Optional.empty();
        }
        List<Evaluation> toutes = evaluationDao.findByIdEnseignement(evaluation.idEnseignement());
        Evaluation precedente = null;
        for (Evaluation ev : toutes) {
            if (ev.id().equals(evaluation.id())) {
                break;
            }
            precedente = ev;
        }
        return Optional.ofNullable(precedente);
    }

    // ------------------------------------------------------------------
    // Statistiques
    // ------------------------------------------------------------------

    /** Statistiques d'une évaluation, calculées à partir des notes déjà saisies. */
    public StatsEvaluation statistiquesEvaluation(Long idEvaluation) throws Exception {
        Evaluation evaluation = evaluationDao.findById(idEvaluation);
        if (evaluation == null) {
            throw new IllegalArgumentException("Évaluation introuvable : " + idEvaluation);
        }
        var enseignement = DaoFactory.getInstance().getEnseignementDao().findById(evaluation.idEnseignement());
        Long idClasse = enseignement == null ? null : enseignement.idClasse();
        List<Etudiant> etudiants = idClasse == null
            ? List.of()
            : DaoFactory.getInstance().getEtudiantDao().findByIdClasse(idClasse);

        Map<Long, Double> notes = new LinkedHashMap<>();
        for (Note note : noteDao.findByIdEvaluation(idEvaluation)) {
            if (note.valeur() != null) {
                notes.put(note.idEtudiant(), note.valeur());
            }
        }
        return statistiques(etudiants, notes, evaluation.bareme());
    }

    /**
     * Statistiques descriptives d'un lot de notes ramenées sur 20.
     *
     * @param etudiants liste des élèves attendus
     * @param notes    notes par identifiant d'étudiant, sur le barème de l'évaluation
     * @param bareme   barème de l'évaluation
     */
    public StatsEvaluation statistiques(List<Etudiant> etudiants, Map<Long, Double> notes, double bareme) {
        int effectif = etudiants.size();
        List<Double> sur20 = new ArrayList<>(notes.size());
        List<Etudiant> sansNote = new ArrayList<>();
        for (Etudiant e : etudiants) {
            Double valeur = notes.get(e.id());
            if (valeur == null) {
                sansNote.add(e);
            } else {
                sur20.add(valeur * 20.0 / (bareme <= 0 ? 20.0 : bareme));
            }
        }
        if (sur20.isEmpty()) {
            return new StatsEvaluation(0, effectif, null, null, null, null, null, 0,
                List.of(), sansNote, bareme);
        }

        Collections.sort(sur20);
        double somme = 0;
        for (double v : sur20) {
            somme += v;
        }
        double moyenne = somme / sur20.size();
        double variance = 0;
        for (double v : sur20) {
            variance += (v - moyenne) * (v - moyenne);
        }
        double ecartType = Math.sqrt(variance / sur20.size());

        int medianeIndex = sur20.size() / 2;
        double mediane = sur20.size() % 2 == 1
            ? sur20.get(medianeIndex)
            : (sur20.get(medianeIndex - 1) + sur20.get(medianeIndex)) / 2.0;

        long reussites = sur20.stream().filter(v -> v >= SEUIL_REUSSITE).count();
        double tauxReussite = (double) reussites / sur20.size();

        List<Integer> histogramme = new ArrayList<>(TRANCHES);
        for (int i = 0; i < TRANCHES; i++) {
            histogramme.add(0);
        }
        for (double v : sur20) {
            int tranche = (int) Math.floor(v / (20.0 / TRANCHES));
            tranche = Math.max(0, Math.min(TRANCHES - 1, tranche));
            histogramme.set(tranche, histogramme.get(tranche) + 1);
        }

        return new StatsEvaluation(sur20.size(), effectif, moyenne, sur20.get(0),
            sur20.get(sur20.size() - 1), mediane, ecartType, tauxReussite, histogramme, sansNote, bareme);
    }

    /** Synthèse des moyennes d'une classe, via le service de moyennes. */
    public List<MoyenneService.SyntheseEtudiant> syntheseClasse(Long idClasse) {
        moyenneService.invaliderCache(idClasse);
        return moyenneService.syntheseClasse(idClasse);
    }

    public MoyenneService moyenneService() {
        return moyenneService;
    }

    // ------------------------------------------------------------------
    // Évaluations
    // ------------------------------------------------------------------

    /** Crée une évaluation en validant ses paramètres. */
    public Evaluation creerEvaluation(Long idEnseignement, String libelle,
            Evaluation.TypeEvaluation type, LocalDate date, double poids, double bareme) throws Exception {
        Evaluation evaluation = valider(idEnseignement, libelle, type, date, poids, bareme);
        return evaluationDao.create(evaluation);
    }

    /** Met à jour une évaluation existante. */
    public Evaluation modifierEvaluation(Evaluation existante, String libelle,
            Evaluation.TypeEvaluation type, LocalDate date, double poids, double bareme) throws Exception {
        if (existante == null || existante.id() == null) {
            throw new IllegalArgumentException("Évaluation à modifier introuvable.");
        }
        Evaluation evaluation = valider(existante.idEnseignement(), libelle, type, date, poids, bareme);
        Evaluation miseAJour = new Evaluation(existante.id(), evaluation.libelle(), evaluation.type(),
            evaluation.date(), evaluation.idEnseignement(), evaluation.poids(), evaluation.bareme());
        evaluationDao.update(miseAJour);
        return miseAJour;
    }

    /**
     * Supprime une évaluation et toutes ses notes (cascade SQLite).
     *
     * @return nombre de notes supprimées par la cascade
     */
    public int supprimerEvaluation(Long idEvaluation) throws Exception {
        int avant = noteDao.findByIdEvaluation(idEvaluation).size();
        evaluationDao.delete(idEvaluation);
        invaliderCaches();
        return avant;
    }

    /** Nombre de notes déjà saisies pour une évaluation. */
    public int compterNotes(Long idEvaluation) throws Exception {
        return noteDao.findByIdEvaluation(idEvaluation).size();
    }

    /**
     * Compte les notes de plusieurs évaluations en une seule requête, pour
     * alimenter le catalogue des évaluations sans requête par ligne.
     */
    public Map<Long, Integer> compterNotesParEvaluation(List<Long> idsEvaluations) throws Exception {
        return noteDao.compterNotesParEvaluation(idsEvaluations);
    }

    private static Evaluation valider(Long idEnseignement, String libelle,
            Evaluation.TypeEvaluation type, LocalDate date, double poids, double bareme) {
        if (idEnseignement == null) {
            throw new IllegalArgumentException("Aucun enseignement sélectionné.");
        }
        if (libelle == null || libelle.isBlank()) {
            throw new IllegalArgumentException("Le libellé de l'évaluation est obligatoire.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Le type de l'évaluation est obligatoire.");
        }
        if (poids < 0 || poids > 100) {
            throw new IllegalArgumentException("Le poids doit être compris entre 0 et 100.");
        }
        if (bareme <= 0) {
            throw new IllegalArgumentException("Le barème doit être supérieur à 0.");
        }
        return new Evaluation(null, libelle.trim(), type, date, idEnseignement, poids, bareme);
    }

    private void invaliderCaches() {
        moyenneService.invaliderTout();
    }

    // ------------------------------------------------------------------
    // Records
    // ------------------------------------------------------------------

    /** Données d'une grille de saisie. */
    public record GrilleSaisie(
        Evaluation evaluation,
        Long idClasse,
        List<Etudiant> etudiants,
        Map<Long, Double> notes,
        StatsEvaluation statistiques
    ) {
        public GrilleSaisie {
            etudiants = List.copyOf(etudiants);
            notes = Map.copyOf(notes);
        }

        /** Effectif de la classe. */
        public int effectif() {
            return etudiants.size();
        }
    }

    /**
     * Statistiques descriptives d'une évaluation, toutes valeurs ramenées sur 20.
     *
     * @param notesSaisies   nombre d'étudiants notés
     * @param effectif       nombre d'étudents attendus
     * @param moyenne        moyenne sur 20
     * @param minimum        note la plus basse sur 20
     * @param maximum        note la plus haute sur 20
     * @param mediane        médiane sur 20
     * @param ecartType      écart-type sur 20
     * @param tauxReussite   proportion d'élèves ayant au moins 10, entre 0 et 1
     * @param histogramme    effectifs par tranche de 2 points, de 0-2 à 18-20
     * @param sansNote       étudiants non notés
     * @param bareme         barème de l'évaluation
     */
    public record StatsEvaluation(
        int notesSaisies,
        int effectif,
        Double moyenne,
        Double minimum,
        Double maximum,
        Double mediane,
        Double ecartType,
        double tauxReussite,
        List<Integer> histogramme,
        List<Etudiant> sansNote,
        double bareme
    ) {
        public StatsEvaluation {
            histogramme = List.copyOf(histogramme);
            sansNote = List.copyOf(sansNote);
        }

        /** Nombre d'étudiants non notés. */
        public int nbSansNote() {
            return sansNote.size();
        }

        /** Taux de réussite formaté en pourcentage. */
        public String tauxReussitePourcentage() {
            return String.format("%.0f %%", tauxReussite * 100);
        }

        /** Moyenne formatée, ou un tiret si aucune note. */
        public String moyenneFormatee() {
            return moyenne == null ? "—" : String.format("%.2f", moyenne);
        }
    }

    /** Facade d'écriture isolée, utile pour les tests. */
    public OptionalDouble moyenneGenerale(Long idEtudiant, Long idClasse) {
        return moyenneService.moyenneGenerale(idEtudiant, idClasse);
    }
}
