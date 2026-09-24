package com.monecole.gestion.models;

/**
 * Entité Matière.
 */
public record Matiere(
    Long id,
    String code,
    String libelle,
    double coefficient
) {}
