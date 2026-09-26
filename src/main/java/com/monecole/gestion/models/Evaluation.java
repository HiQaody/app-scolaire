package com.monecole.gestion.models;

import java.time.LocalDate;

/**
 * Entité Évaluation (Devoir, Examen, etc.).
 *
 * @param poids  pondération de l'évaluation dans la moyenne de la matière
 * @param bareme note maximale de l'évaluation (20 par défaut)
 */
public record Evaluation(
    Long id,
    String libelle,
    TypeEvaluation type,
    LocalDate date,
    Long idEnseignement,
    double poids,
    double bareme
) {
    /** Constructeur de compatibilité : poids 1, barème 20. */
    public Evaluation(Long id, String libelle, TypeEvaluation type, LocalDate date, Long idEnseignement) {
        this(id, libelle, type, date, idEnseignement, 1.0, 20.0);
    }

    /** Note ramenée sur 20, quelle que soit l'échelle de l'évaluation. */
    public double noteSur20(Double valeur) {
        if (valeur == null || bareme <= 0) {
            return 0;
        }
        return valeur * 20.0 / bareme;
    }

    /** Vrai si l'évaluation contribue au calcul de la moyenne. */
    public boolean compteDansMoyenne() {
        return poids > 0;
    }

    public enum TypeEvaluation {
        DEVOIR("Devoir"),
        EXAMEN("Examen"),
        PROJECT("Projet"),
        PARTICIPATION("Participation");

        private final String label;

        TypeEvaluation(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static TypeEvaluation fromValue(String value) {
            if (value == null) return null;
            return TypeEvaluation.valueOf(value.trim().toUpperCase());
        }
    }
}
