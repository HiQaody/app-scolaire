package com.monecole.gestion.models;

/**
 * Entité Enseignant.
 */
public record Enseignant(
    Long id,
    String matricule,
    String nom,
    String prenom
) {}
