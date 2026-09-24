package com.monecole.gestion.models;

import java.time.LocalDate;

/**
 * Entité Évaluation (Devoir, Examen, etc.).
 */
public record Evaluation(
    Long id,
    String libelle,
    TypeEvaluation type,
    LocalDate date,
    Long idEnseignement
) {
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
