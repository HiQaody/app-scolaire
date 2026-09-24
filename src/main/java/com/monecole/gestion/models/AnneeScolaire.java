package com.monecole.gestion.models;

import java.time.LocalDate;

/**
 * Entité Année Scolaire.
 */
public record AnneeScolaire(
    Long id,
    String libelle,
    boolean estActive
) {}
