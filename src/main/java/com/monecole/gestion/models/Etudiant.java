package com.monecole.gestion.models;

import java.time.LocalDate;

/**
 * Entité Étudiant.
 */
public record Etudiant(
    Long id,
    String matricule,
    String nom,
    String prenom,
    LocalDate dateNaissance,
    Sexe sexe,
    Long idClasse
) {
    public enum Sexe {
        M, F
    }
}
