package com.monecole.gestion.models;

/**
 * Entité Note.
 */
public record Note(
    Long id,
    Double valeur,
    Long idEtudiant,
    Long idEvaluation
) {}
