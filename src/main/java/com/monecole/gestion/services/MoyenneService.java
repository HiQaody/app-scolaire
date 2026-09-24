package com.monecole.gestion.services;

import com.monecole.gestion.dao.*;
import com.monecole.gestion.models.*;

import java.awt.Color;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service de calcul des moyennes selon le CDC :
 * - Moyenne par matière (pondérée par coefficient)
 * - Moyenne par trimestre
 * - Moyenne annuelle
 * - Rang de classe
 */
public class MoyenneService {

    private static final Logger LOGGER = Logger.getLogger(MoyenneService.class.getName());

    private final EtudiantDao etudiantDao;
    private final ClasseDao classeDao;
    private final MatiereDao matiereDao;
    private final EnseignantDao enseignantDao;
    private final EnseignementDao enseignementDao;
    private final EvaluationDao evaluationDao;
    private final NoteDao noteDao;

    public MoyenneService() {
        DaoFactory df = DaoFactory.getInstance();
        this.etudiantDao = df.getEtudiantDao();
        this.classeDao = df.getClasseDao();
        this.matiereDao = df.getMatiereDao();
        this.enseignantDao = df.getEnseignantDao();
        this.enseignementDao = df.getEnseignementDao();
        this.evaluationDao = df.getEvaluationDao();
        this.noteDao = df.getNoteDao();
    }

    /**
     * Calcule la moyenne d'un étudiant pour une matière spécifique.
     * Moyenne simple de toutes les notes de cette matière.
     */
    public OptionalDouble moyenneMatiere(Long idEtudiant, Long idMatiere, Long idClasse) {
        try {
            List<Enseignement> ens = enseignementDao.findByIdClasse(idClasse);
            List<Long> notes = new ArrayList<>();
            for (Enseignement e : ens) {
                if (!e.idMatiere().equals(idMatiere)) continue;
                List<Evaluation> evals = evaluationDao.findByIdEnseignement(e.id());
                for (Evaluation ev : evals) {
                    Note n = noteDao.findByIdEtudiantAndEvaluation(idEtudiant, ev.id());
                    if (n != null && n.valeur() != null) {
                        notes.add(n.valeur().longValue());
                    }
                }
            }
            return notes.stream().mapToLong(Long::longValue).average();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur calcul moyenne matière", e);
            return OptionalDouble.empty();
        }
    }

    /**
     * Calcule la moyenne générale pondérée d'un étudiant dans une classe.
     * (moyenne de matière × coefficient) / somme des coefficients
     */
    public OptionalDouble moyenneGenerale(Long idEtudiant, Long idClasse) {
        try {
            List<Enseignement> ens = enseignementDao.findByIdClasse(idClasse);
            double sommeProduits = 0;
            double sommeCoeffs = 0;

            for (Enseignement e : ens) {
                Matiere m = matiereDao.findById(e.idMatiere());
                OptionalDouble moyenne = moyenneMatiere(idEtudiant, e.idMatiere(), idClasse);
                if (moyenne.isPresent()) {
                    sommeProduits += moyenne.getAsDouble() * m.coefficient();
                    sommeCoeffs += m.coefficient();
                }
            }
            if (sommeCoeffs == 0) return OptionalDouble.empty();
            return OptionalDouble.of(sommeProduits / sommeCoeffs);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur calcul moyenne générale", e);
            return OptionalDouble.empty();
        }
    }

    /**
     * Calcule la moyenne d'une classe (moyenne de tous les étudiants).
     */
    public OptionalDouble moyenneClasse(Long idClasse) {
        try {
            List<Etudiant> etudiants = etudiantDao.findByIdClasse(idClasse);
            double sum = 0;
            int count = 0;
            for (Etudiant e : etudiants) {
                OptionalDouble moy = moyenneGenerale(e.id(), idClasse);
                if (moy.isPresent()) {
                    sum += moy.getAsDouble();
                    count++;
                }
            }
            return count > 0 ? OptionalDouble.of(sum / count) : OptionalDouble.empty();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur calcul moyenne classe", e);
            return OptionalDouble.empty();
        }
    }

    /**
     * Calcule les moyennes de tous les étudiants d'une classe, triées par ordre décroissant.
     * Retourne une liste de (étudiant, moyenne) pour le classement.
     */
    public List<EtudiantMoyenne> classementClasse(Long idClasse) {
        try {
            List<Etudiant> etudiants = etudiantDao.findByIdClasse(idClasse);
            List<EtudiantMoyenne> results = new ArrayList<>();
            for (Etudiant e : etudiants) {
                OptionalDouble moy = moyenneGenerale(e.id(), idClasse);
                double m = moy.orElse(-1);
                results.add(new EtudiantMoyenne(e, m));
            }
            results.sort((a, b) -> Double.compare(b.moyenne(), a.moyenne()));
            return results;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur calcul classement", e);
            return List.of();
        }
    }

    /**
     * Calcule la moyenne par matière pour une classe (pour un graphique).
     */
    public Map<String, Double> moyennesParMatiere(Long idClasse) {
        Map<String, Double> result = new LinkedHashMap<>();
        try {
            List<Enseignement> ens = enseignementDao.findByIdClasse(idClasse);
            for (Enseignement e : ens) {
                Matiere m = matiereDao.findById(e.idMatiere());
                OptionalDouble moy = moyenneClasseParMatiere(idClasse, e.idMatiere());
                result.put(m.libelle() + " (" + m.code() + ")", moy.orElse(-1.0));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur calcul moyennes par matière", e);
        }
        return result;
    }

    private OptionalDouble moyenneClasseParMatiere(Long idClasse, Long idMatiere) {
        try {
            List<Etudiant> etudiants = etudiantDao.findByIdClasse(idClasse);
            List<Double> notes = new ArrayList<>();
            for (Etudiant e : etudiants) {
                OptionalDouble moy = moyenneMatiere(e.id(), idMatiere, idClasse);
                moy.ifPresent(notes::add);
            }
            return notes.stream().mapToDouble(Double::doubleValue).average();
        } catch (Exception e) {
            return OptionalDouble.empty();
        }
    }

    /**
     * Calcule la moyenne par trimestre.
     * Les trimestres sont définis par les mois : S1 (sep-dec), S2 (jan-avr), S3 (mai-août).
     */
    public OptionalDouble moyenneParTrimestre(Long idEtudiant, Long idClasse, Trimestre trimestre) {
        try {
            List<Enseignement> ens = enseignementDao.findByIdClasse(idClasse);
            double sommeProduits = 0;
            double sommeCoeffs = 0;

            for (Enseignement e : ens) {
                Matiere m = matiereDao.findById(e.idMatiere());
                List<Evaluation> evals = evaluationDao.findByIdEnseignement(e.id());
                List<Double> notesTrim = new ArrayList<>();
                for (Evaluation ev : evals) {
                    if (ev.date() != null && trimestre.contains(ev.date())) {
                        Note n = noteDao.findByIdEtudiantAndEvaluation(idEtudiant, ev.id());
                        if (n != null && n.valeur() != null) {
                            notesTrim.add(n.valeur());
                        }
                    }
                }
                if (!notesTrim.isEmpty()) {
                    double moyMatiere = notesTrim.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    sommeProduits += moyMatiere * m.coefficient();
                    sommeCoeffs += m.coefficient();
                }
            }
            if (sommeCoeffs == 0) return OptionalDouble.empty();
            return OptionalDouble.of(sommeProduits / sommeCoeffs);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur calcul moyenne trimestre", e);
            return OptionalDouble.empty();
        }
    }

    /**
     * Détermine la mention d'un étudiant selon sa moyenne générale.
     */
    public static Mention getMention(double moyenne) {
        if (moyenne < 10) return Mention.ECHEC;
        if (moyenne < 12) return Mention.PASSABLE;
        if (moyenne < 14) return Mention.ASSEZ_BIEN;
        if (moyenne < 16) return Mention.BIEN;
        return Mention.TRES_BIEN;
    }

    /**
     * Calcule la répartition des mentions pour une classe.
     */
    public Map<Mention, Integer> repartitionMentions(Long idClasse) {
        Map<Mention, Integer> result = new EnumMap<>(Mention.class);
        for (Mention m : Mention.values()) {
            result.put(m, 0);
        }
        try {
            List<Etudiant> etudiants = etudiantDao.findByIdClasse(idClasse);
            for (Etudiant e : etudiants) {
                OptionalDouble moy = moyenneGenerale(e.id(), idClasse);
                if (moy.isPresent()) {
                    Mention mention = getMention(moy.getAsDouble());
                    result.put(mention, result.get(mention) + 1);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur calcul répartition mentions", e);
        }
        return result;
    }

    /**
     * Calcule le taux de réussite d'une classe.
     */
    public double tauxReussite(Long idClasse) {
        try {
            List<Etudiant> etudiants = etudiantDao.findByIdClasse(idClasse);
            long reussite = etudiants.stream()
                .mapToDouble(e -> moyenneGenerale(e.id(), idClasse).orElse(-1))
                .filter(m -> m >= 10)
                .count();
            return etudiants.isEmpty() ? 0 : (double) reussite / etudiants.size();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur calcul taux réussite", e);
            return 0;
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
            return months.contains(date.getMonth());
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

        public String getLabel() { return label; }
        public Color getColor() { return color; }
    }

    /**
     * Record pour representer un étudiant avec sa moyenne.
     */
    public record EtudiantMoyenne(Etudiant etudiant, double moyenne) {}
}
