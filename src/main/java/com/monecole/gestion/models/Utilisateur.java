package com.monecole.gestion.models;

/**
 * Entité Utilisateur.
 * Représente un compte utilisateur avec un rôle métier.
 */
public record Utilisateur(
    Long id,
    String login,
    String passwordHash,
    Role role,
    Long idEnseignant
) {
    public enum Role {
        ADMIN("Administrateur"),
        SECRETAIRE("Secrétaire Pédagogique"),
        ENSEIGNANT("Enseignant");

        private final String label;

        Role(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static Role fromValue(String value) {
            if (value == null) return null;
            return Role.valueOf(value.trim().toUpperCase());
        }
    }
}
