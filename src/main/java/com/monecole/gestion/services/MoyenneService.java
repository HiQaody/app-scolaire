package com.monecole.gestion.services;

import com.monecole.gestion.dao.ClasseDao;
import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.dao.EtudiantDao;
import com.monecole.gestion.dao.NoteDao;
import com.monecole.gestion.models.Etudiant;
import com.monecole.gestion.models.NoteDetail;

import java.awt.Color;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * Service de calcul des moyennes selon le CDC :
 * - Moyenne par matière, pondérée par le poids de chaque évaluation
 * - Moyenne générale, pondérée par le coefficient de chaque matière
 * - Moyenne par trimestre
 * - Rang de classe et mention
 * <p>
 * Toutes les notes d'une classe sont chargées en une seule requête puis regroupées
 * en mémoire : les moyennes ne coûtent plus N requêtes mais une, ce qui rend
 * l'affichage du bulletin et de la synthèse instantané.
 * <p>
 * Les notes sont ramenées sur 20 via le barème de leur évaluation avant pondération.
 * Le cache est invalidé explicitement après toute écriture (voir
 * {@link #invaliderCache(Long)}).
 */
public class MoyenneService {

    private static final Logger LOGGER = Logger.getLogger(MoyenneService.class.getName());

    private final EtudiantDao etudiantDao;
    private final ClasseDao classeDao;
    private final NoteDao noteDao;

    /** Cache par classe, invalidé après chaque écriture de notes. */
    private final Map<Long, SnapshotClasse> cache = new ConcurrentHashMap<>();

    public MoyenneService() {
        DaoFactory df = DaoFactory.getInstance();
        this.etudiantDao = df.getEtudiantDao();
        this.classeDao = df.getClasseDao();
        this.noteDao = df.getNoteDao();
    }

    /** Invalide le cache d'une classe après une modification de ses notes. */
    public void invaliderCache(Long idClasse) {
        if (idClasse != null) {
            cache.remove(idClasse);
        }
    }

    /** Vide tout le cache. */
    public void invaliderTout() {
        cache.clear();
    }

    // ------------------------------------------------------------------
    // Moyennes unitaire
    // ------------------------------------------------------------------

    /**
     * Moyenne d'un étudiant pour une matière : moyenne pondérée des évaluations,
     * chaque note étant ramenée sur 20 via son barème.
     */
    public OptionalDouble moyenneMatiere(Long idEtudiant, Long idMatiere, Long idClasse) {
        return moyenneMatiereDuCache(idClasse, idEtudiant, idMatiere);
    }

    /** Moyenne d'un étudiant dans une matière, pour la classe par défaut de l'étudiant. */
    public OptionalDouble moyenneMatiere(Long idEtudiant, Long idMatiere) {
        Long idClasse = classeDe(idEtudiant);
        return idClasse == null
            ? OptionalDouble.empty()
            : moyenneMatiereDuCache(idClasse, idEtudiant, idMatiere);
    }

    /** Moyenne générale d'un étudiant : (moyenne matière × coefficient) / Σ coefficients. */
    public OptionalDouble moyenneGenerale(Long idEtudiant, Long idClasse) {
        SnapshotClasse snapshot = snapshot(idClasse);
        Map<Long, Double> parMatiere = moyennesMatiereParEtudiantDansSnapshot(snapshot, idEtudiant);
        if (parMatiere.isEmpty()) {
            return OptionalDouble.empty();
        }
        double moyenne = moyenneGeneralePonderee(parMatiere, snapshot.coefficientsParMatiere());
        return Double.isNaN(moyenne) ? OptionalDouble.empty() : OptionalDouble.of(moyenne);
    }

    /** Moyenne générale d'un étudiant dans la classe à laquelle il appartient. */
    public OptionalDouble moyenneGenerale(Long idEtudiant) {
        Long idClasse = classeDe(idEtudiant);
        return idClasse == null ? OptionalDouble.empty() : moyenneGenerale(idEtudiant, idClasse);
    }

    /** Moyennes d'un étudiant par matière, dans l'ordre des matières rencontrées. */
    public Map<Long, Double> moyennesMatiereParEtudiant(Long idEtudiant, Long idClasse) {
        SnapshotClasse snapshot = snapshot(idClasse);
        Map<Long, Double> resultat = new LinkedHashMap<>();
        for (Map.Entry<Long, List<NoteDetail>> entry : snapshot.notesParMatiere().entrySet()) {
            double moyenne = moyennePonderee(entry.getValue(), idEtudiant);
            if (!Double.isNaN(moyenne)) {
                resultat.put(entry.getKey(), moyenne);
            }
        }
        return resultat;
    }

    /** Moyenne de la classe : moyenne des moyennes générales des étudiants notés. */
    public OptionalDouble moyenneClasse(Long idClasse) {
        SnapshotClasse snapshot = snapshot(idClasse);
        double somme = 0;
        int count = 0;
        for (Etudiant e : snapshot.etudiants()) {
            Map<Long, Double> parMatiere = moyennesMatiereParEtudiantDansSnapshot(snapshot, e.id());
            if (parMatiere.isEmpty()) {
                continue;
            }
            somme += moyenneGeneralePonderee(parMatiere, snapshot.coefficientsParMatiere());
            count++;
        }
        return count == 0 ? OptionalDouble.empty() : OptionalDouble.of(somme / count);
    }

    /** Classement de la classe par moyenne générale décroissante, sans note en dernier. */
    public List<EtudiantMoyenne> classementClasse(Long idClasse) {
        List<SyntheseEtudiant> synthese = syntheseClasse(idClasse);
        List<EtudiantMoyenne> resultat = new ArrayList<>(synthese.size());
        for (SyntheseEtudiant s : synthese) {
            resultat.add(new EtudiantMoyenne(s.etudiant(), s.moyenneGenerale()));
        }
        return resultat;
    }

    /** Synthèse complète de la classe : moyenne par matière, moyenne générale, rang, mention. */
    public List<SyntheseEtudiant> syntheseClasse(Long idClasse) {
        SnapshotClasse snapshot = snapshot(idClasse);
        List<SyntheseEtudiant> resultat = new ArrayList<>();
        for (Etudiant e : snapshot.etudiants()) {
            Map<Long, Double> parMatiere = moyennesMatiereParEtudiantDansSnapshot(snapshot, e.id());
            double moyenne = parMatiere.isEmpty()
                ? Double.NaN
                : moyenneGeneralePonderee(parMatiere, snapshot.coefficientsParMatiere());
            resultat.add(new SyntheseEtudiant(e, parMatiere, moyenne, 0,
                parMatiere.isEmpty() || Double.isNaN(moyenne) ? null : getMention(moyenne)));
        }
        resultat.sort(Comparator
            .comparingDouble((SyntheseEtudiant s) -> Double.isNaN(s.moyenneGenerale())
                ? Double.MAX_VALUE : -s.moyenneGenerale())
            .thenComparing(s -> s.etudiant().nom() + s.etudiant().prenom()));
        return attribuerRangs(resultat);
    }

    /** Réattribue les rangs par moyenne décroissante, ex æquo à rang égal. */
    private static List<SyntheseEtudiant> attribuerRangs(List<SyntheseEtudiant> tries) {
        List<SyntheseEtudiant> resultat = new ArrayList<>(tries.size());
        int rangCourant = 0;
        Double moyennePrecedente = null;
        for (int i = 0; i < tries.size(); i++) {
            SyntheseEtudiant s = tries.get(i);
            boolean memeMoyenne = moyennePrecedente != null
                && !Double.isNaN(s.moyenneGenerale())
                && Math.abs(s.moyenneGenerale() - moyennePrecedente) < 1e-9;
            if (!memeMoyenne) {
                rangCourant = Double.isNaN(s.moyenneGenerale()) ? 0 : i + 1;
                moyennePrecedente = Double.isNaN(s.moyenneGenerale()) ? null : s.moyenneGenerale();
            }
            resultat.add(new SyntheseEtudiant(s.etudiant(), s.moyennesParMatiere(),
                s.moyenneGenerale(), rangCourant, s.mention()));
        }
        return resultat;
    }

    /** Moyenne de la classe par matière, pour un graphique. */
    public Map<String, Double> moyennesParMatiere(Long idClasse) {
        SnapshotClasse snapshot = snapshot(idClasse);
        Map<String, Double> resultat = new LinkedHashMap<>();
        for (Map.Entry<Long, List<NoteDetail>> entry : snapshot.notesParMatiere().entrySet()) {
            List<NoteDetail> notes = entry.getValue();
            if (notes.isEmpty()) {
                continue;
            }
            double somme = 0;
            int count = 0;
            for (Etudiant e : snapshot.etudiants()) {
                double moyenne = moyennePonderee(notes, e.id());
                if (!Double.isNaN(moyenne)) {
                    somme += moyenne;
                    count++;
                }
            }
            if (count > 0) {
                resultat.put(libelleMatiere(notes.get(0)), somme / count);
            }
        }
        return resultat;
    }

    /**
     * Moyenne d'un étudiant sur un trimestre : seules les évaluations du trimestre
     * sont prises en compte, la pondération par matière restant inchangée.
     */
    public OptionalDouble moyenneParTrimestre(Long idEtudiant, Long idClasse, Trimestre trimestre) {
        SnapshotClasse snapshot = snapshot(idClasse);
        double sommeProduits = 0;
        double sommeCoefficients = 0;
        for (Map.Entry<Long, List<NoteDetail>> entry : snapshot.notesParMatiere().entrySet()) {
            List<NoteDetail> notes = entry.getValue();
            List<NoteDetail> duTrimestre = new ArrayList<>();
            for (NoteDetail n : notes) {
                if (n.idEtudiant().equals(idEtudiant) && n.dateEvaluation() != null
                    && trimestre.contains(n.dateEvaluation())) {
                    duTrimestre.add(n);
                }
            }
            double moyenne = moyennePonderee(duTrimestre, idEtudiant);
            if (Double.isNaN(moyenne)) {
                continue;
            }
            sommeProduits += moyenne * coefficient(notes);
            sommeCoefficients += coefficient(notes);
        }
        return sommeCoefficients == 0
            ? OptionalDouble.empty()
            : OptionalDouble.of(sommeProduits / sommeCoefficients);
    }

    // ------------------------------------------------------------------
    // Statistiques de mention
    // ------------------------------------------------------------------

    /** Détermine la mention d'un étudiant selon sa moyenne générale sur 20. */
    public static Mention getMention(double moyenne) {
        if (Double.isNaN(moyenne) || moyenne < 10) return Mention.ECHEC;
        if (moyenne < 12) return Mention.PASSABLE;
        if (moyenne < 14) return Mention.ASSEZ_BIEN;
        if (moyenne < 16) return Mention.BIEN;
        return Mention.TRES_BIEN;
    }

    /** Répartition des mentions de la classe. */
    public Map<Mention, Integer> repartitionMentions(Long idClasse) {
        Map<Mention, Integer> resultat = new EnumMap<>(Mention.class);
        for (Mention m : Mention.values()) {
            resultat.put(m, 0);
        }
        for (SyntheseEtudiant s : syntheseClasse(idClasse)) {
            if (s.mention() != null) {
                resultat.merge(s.mention(), 1, Integer::sum);
            }
        }
        return resultat;
    }

    /** Taux de réussite de la classe : proportion d'étudiants ayant au moins 10 de moyenne. */
    public double tauxReussite(Long idClasse) {
        List<SyntheseEtudiant> synthese = syntheseClasse(idClasse);
        if (synthese.isEmpty()) {
            return 0;
        }
        long reussite = synthese.stream()
            .filter(s -> !Double.isNaN(s.moyenneGenerale()) && s.moyenneGenerale() >= 10)
            .count();
        return (double) reussite / synthese.size();
    }

    // ------------------------------------------------------------------
    // Calculs internes
    // ------------------------------------------------------------------

    /**
     * Moyenne pondérée des évaluations d'un étudiant dans une liste de notes.
     * Chaque note est ramenée sur 20 puis pondérée par le poids de son évaluation.
     *
     * @return la moyenne, ou {@code Double.NaN} si l'étudiant n'a aucune note
     */
    private static double moyennePonderee(List<NoteDetail> notes, Long idEtudiant) {
        double somme = 0;
        double sommePoids = 0;
        for (NoteDetail n : notes) {
            if (!n.idEtudiant().equals(idEtudiant) || n.valeur() == null) {
                continue;
            }
            somme += n.valeurSur20() * n.poids();
            sommePoids += n.poids();
        }
        return sommePoids == 0 ? Double.NaN : somme / sommePoids;
    }

    /** (Σ moyenneMatiere × coefficient) / Σ coefficient sur les matières notées. */
    private static double moyenneGeneralePonderee(Map<Long, Double> moyennesParMatiere,
            Map<Long, Double> coefficientsParMatiere) {
        double somme = 0;
        double sommeCoefficients = 0;
        for (Map.Entry<Long, Double> entry : moyennesParMatiere.entrySet()) {
            double coefficient = coefficientsParMatiere.getOrDefault(entry.getKey(), 1.0);
            somme += entry.getValue() * coefficient;
            sommeCoefficients += coefficient;
        }
        return sommeCoefficients == 0 ? Double.NaN : somme / sommeCoefficients;
    }

    private static double coefficient(List<NoteDetail> notes) {
        return notes.isEmpty() ? 1.0 : notes.get(0).coefficient();
    }

    private static String libelleMatiere(NoteDetail n) {
        return n.libelleMatiere() + " (" + n.codeMatiere() + ")";
    }

    private OptionalDouble moyenneMatiereDuCache(Long idClasse, Long idEtudiant, Long idMatiere) {
        List<NoteDetail> notes = snapshot(idClasse).notesParMatiere().get(idMatiere);
        if (notes == null) {
            return OptionalDouble.empty();
        }
        double moyenne = moyennePonderee(notes, idEtudiant);
        return Double.isNaN(moyenne) ? OptionalDouble.empty() : OptionalDouble.of(moyenne);
    }

    private Map<Long, Double> moyennesMatiereParEtudiantDansSnapshot(SnapshotClasse snapshot, Long idEtudiant) {
        Map<Long, Double> resultat = new LinkedHashMap<>();
        for (Map.Entry<Long, List<NoteDetail>> entry : snapshot.notesParMatiere().entrySet()) {
            double moyenne = moyennePonderee(entry.getValue(), idEtudiant);
            if (!Double.isNaN(moyenne)) {
                resultat.put(entry.getKey(), moyenne);
            }
        }
        return resultat;
    }

    private Long classeDe(Long idEtudiant) {
        try {
            Etudiant e = etudiantDao.findById(idEtudiant);
            return e == null ? null : e.idClasse();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur résolution classe de l'étudiant " + idEtudiant, e);
            return null;
        }
    }

    /** Charge (ou réutilise) l'instantané des notes d'une classe. */
    private SnapshotClasse snapshot(Long idClasse) {
        if (idClasse == null) {
            return SnapshotClasse.vide();
        }
        return cache.computeIfAbsent(idClasse, this::chargerSnapshot);
    }

    private SnapshotClasse chargerSnapshot(Long idClasse) {
        try {
            List<Etudiant> etudiants = etudiantDao.findByIdClasse(idClasse);
            List<NoteDetail> notes = noteDao.findByIdClasse(idClasse);

            Map<Long, Etudiant> etudiantsParId = new LinkedHashMap<>();
            for (Etudiant e : etudiants) {
                etudiantsParId.put(e.id(), e);
            }

            Map<Long, List<NoteDetail>> parEtudiant = new LinkedHashMap<>();
            Map<Long, List<NoteDetail>> parMatiere = new LinkedHashMap<>();
            Map<Long, Double> coefficients = new LinkedHashMap<>();
            for (NoteDetail n : notes) {
                coefficients.putIfAbsent(n.idMatiere(), n.coefficient());
                if (n.valeur() == null) {
                    continue;
                }
                parEtudiant.computeIfAbsent(n.idEtudiant(), k -> new ArrayList<>()).add(n);
                parMatiere.computeIfAbsent(n.idMatiere(), k -> new ArrayList<>()).add(n);
            }

            return new SnapshotClasse(etudiants, etudiantsParId, notes, parEtudiant, parMatiere, coefficients);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement des notes de la classe " + idClasse, e);
            return SnapshotClasse.vide();
        }
    }

    /**
     * Photo des notes d'une classe, regroupée par étudiant et par matière.
     * Le calcul des moyennes ne fait plus aucune requête.
     */
    public record SnapshotClasse(
        List<Etudiant> etudiants,
        Map<Long, Etudiant> etudiantsParId,
        List<NoteDetail> notes,
        Map<Long, List<NoteDetail>> notesParEtudiant,
        Map<Long, List<NoteDetail>> notesParMatiere,
        Map<Long, Double> coefficientsParMatiere
    ) {
        public SnapshotClasse {
            etudiants = List.copyOf(etudiants);
            notes = List.copyOf(notes);
            etudiantsParId = Map.copyOf(etudiantsParId);
            notesParEtudiant = Map.copyOf(notesParEtudiant);
            notesParMatiere = Map.copyOf(notesParMatiere);
            coefficientsParMatiere = Map.copyOf(coefficientsParMatiere);
        }

        /** Snapshot vide, utilisé quand la classe est inconnue ou en cas d'erreur. */
        public static SnapshotClasse vide() {
            return new SnapshotClasse(List.of(), Map.of(), List.of(), Map.of(), Map.of(), Map.of());
        }
    }

    /** Un étudiant avec sa moyenne générale. */
    public record EtudiantMoyenne(Etudiant etudiant, double moyenne) {}

    /**
     * Synthèse d'un étudiant : moyennes par matière, moyenne générale pondérée,
     * rang dans la classe et mention. Le rang est attribué par
     * {@link #syntheseClasse(Long)}, les ex æquo partageant le même rang.
     */
    public record SyntheseEtudiant(
        Etudiant etudiant,
        Map<Long, Double> moyennesParMatiere,
        double moyenneGenerale,
        int rang,
        Mention mention
    ) {
        public SyntheseEtudiant {
            moyennesParMatiere = Map.copyOf(moyennesParMatiere);
        }

        /** Vrai si l'étudiant n'a aucune note dans la classe. */
        public boolean sansNote() {
            return Double.isNaN(moyenneGenerale);
        }
    }

    public enum Trimestre {
        S1(Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER),
        S2(Month.JANUARY, Month.FEBRUARY, Month.MARCH, Month.APRIL),
        S3(Month.MAY, Month.JUNE, Month.JULY, Month.AUGUST);

        private final Set<Month> months;

        Trimestre(Month... months) {
            this.months = EnumSet.noneOf(Month.class);
            for (Month m : months) this.months.add(m);
        }

        public boolean contains(LocalDate date) {
            return date != null && months.contains(date.getMonth());
        }

        public String getLabel() {
            return name();
        }
    }

    public enum Mention {
        ECHEC("Échec", new Color(220, 53, 69)),
        PASSABLE("Passable", new Color(255, 193, 7)),
        ASSEZ_BIEN("Assez Bien", new Color(255, 193, 7)),
        BIEN("Bien", new Color(40, 167, 69)),
        TRES_BIEN("Très Bien", new Color(0, 123, 255));

        private final String label;
        private final Color color;

        Mention(String label, Color color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public Color getColor() {
            return color;
        }
    }
}
