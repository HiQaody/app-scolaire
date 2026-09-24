package com.monecole.gestion.models;

/**
 * Entité Enseignement (table de liaison Enseignant-Matière-Classe).
 */
public record Enseignement(
    Long id,
    Long idEnseignant,
    Long idMatiere,
    Long idClasse
) {}
