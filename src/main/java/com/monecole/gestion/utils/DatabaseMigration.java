package com.monecole.gestion.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Migrations de schéma idempotentes appliquées au démarrage.
 * <p>
 * Le fichier {@code schema.sql} n'est exécuté que lors de la création de la base.
 * Les bases existantes doivent donc être mises à niveau ici, via
 * {@code PRAGMA table_info} + {@code ALTER TABLE}, de manière idempotente.
 */
public final class DatabaseMigration {

    private static final Logger LOGGER = Logger.getLogger(DatabaseMigration.class.getName());

    private DatabaseMigration() {
    }

    /** Applique toutes les migrations sur la connexion courante. */
    public static void apply(Connection conn) {
        try {
            Set<String> colonnesEvaluations = columnsOf(conn, "evaluations");
            if (!colonnesEvaluations.contains("poids")) {
                execute(conn, "ALTER TABLE evaluations ADD COLUMN poids REAL NOT NULL DEFAULT 1");
                LOGGER.info("Migration : colonne evaluations.poids ajoutée.");
            }
            if (!colonnesEvaluations.contains("bareme")) {
                execute(conn, "ALTER TABLE evaluations ADD COLUMN bareme REAL NOT NULL DEFAULT 20");
                LOGGER.info("Migration : colonne evaluations.bareme ajoutée.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Échec des migrations de schéma", e);
        }
    }

    /** Liste les tables présentes, utile pour les vérifications et diagnostics. */
    public static List<String> listTables(Connection conn) {
        List<String> tables = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table'")) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Impossible de lister les tables", e);
        }
        return tables;
    }

    /** Colonnes existantes d'une table. */
    private static Set<String> columnsOf(Connection conn, String table) throws SQLException {
        Set<String> colonnes = new HashSet<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                colonnes.add(rs.getString("name"));
            }
        }
        return colonnes;
    }

    private static void execute(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}
