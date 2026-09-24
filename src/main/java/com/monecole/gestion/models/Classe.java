package com.monecole.gestion.models;

/**
 * Entité Classe.
 */
public record Classe(
    Long id,
    String nom,
    String niveau,
    Long idAnnee
) {}
