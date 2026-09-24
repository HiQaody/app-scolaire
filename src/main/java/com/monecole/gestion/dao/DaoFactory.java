package com.monecole.gestion.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestionnaire singleton de la connexion SQLite.
 * Initialise la base de données à partir de schema.sql si elle n'existe pas.
 */
public class DaoFactory {

    private static final Logger LOGGER = Logger.getLogger(DaoFactory.class.getName());
    private static final String DB_NAME = "ecole.db";
    private static final String DB_DIR = System.getProperty("user.home") + "/.ecole-gestion";
    private static final String DB_URL = "jdbc:sqlite:" + DB_DIR + "/" + DB_NAME;
    private static final String SCHEMA_PATH = "/db/schema.sql";

    private static DaoFactory instance;
    private Connection connection;

    private UtilisateurDao utilisateurDao;
    private AnneeScolaireDao anneeScolaireDao;
    private ClasseDao classeDao;
    private EtudiantDao etudiantDao;
    private MatiereDao matiereDao;
    private EnseignantDao enseignantDao;
    private EnseignementDao enseignementDao;
    private EvaluationDao evaluationDao;
    private NoteDao noteDao;

    private DaoFactory() {
        initializeDatabase();
    }

    /**
     * Retourne l'instance singleton du DaoFactory.
     */
    public static synchronized DaoFactory getInstance() {
        if (instance == null) {
            instance = new DaoFactory();
        }
        return instance;
    }

    /**
     * Retourne la connexion SQLite (créée si nécessaire).
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    /**
     * Initialise la base de données : crée le répertoire et exécute le schéma si la DB n'existe pas.
     */
    private void initializeDatabase() {
        try {
            Path dbDir = Paths.get(DB_DIR);
            if (!Files.exists(dbDir)) {
                Files.createDirectories(dbDir);
            }

            Path dbPath = dbDir.resolve(DB_NAME);
            if (Files.notExists(dbPath)) {
                LOGGER.info("Création de la base de données : " + dbPath);
                try {
                    executeSchema();
                } catch (IOException e) {
                    throw new RuntimeException("Impossible de lire le schéma SQL", e);
                }
            }
        } catch (IOException | SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation de la base de données", e);
            throw new RuntimeException("Impossible d'initialiser la base de données", e);
        }
    }

    /**
     * Exécute le script schema.sql pour créer les tables.
     * Découpe le script en instructions individuelles car SQLite JDBC
     * ne gère pas plusieurs DDL dans un seul appel execute().
     */
    private void executeSchema() throws SQLException, IOException {
        try (InputStream is = getClass().getResourceAsStream(SCHEMA_PATH)) {
            if (is == null) {
                throw new SQLException("Fichier schema.sql introuvable dans les ressources : " + SCHEMA_PATH);
            }
            String schema = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            List<String> statements = splitSqlScript(schema);

            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement stmt = conn.createStatement()) {
                for (String sql : statements) {
                    if (!sql.trim().isEmpty()) {
                        stmt.execute(sql);
                    }
                }
                conn.createStatement().execute("PRAGMA foreign_keys = ON");
            }
        }
    }

    /**
     * Découpe un script SQL en instructions individuelles.
     * Supprime les commentaires (-- et /* *​/) puis divise par point-virgule.
     */
    private static List<String> splitSqlScript(String script) {
        String cleaned = script
            .replaceAll("--[^\n]*", "")
            .replaceAll("/\\*[^*]*\\*/", "")
            .trim();
        String[] parts = cleaned.split(";", -1);
        List<String> statements = new ArrayList<>();
        for (String sql : parts) {
            sql = sql.trim();
            if (!sql.isEmpty()) {
                statements.add(sql);
            }
        }
        return statements;
    }

    /**
     * Ferme la connexion courante si elle est ouverte.
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Erreur lors de la fermeture de la connexion", e);
            }
        }
    }

    /**
     * Retourne le chemin absolu du fichier de base de données.
     */
    public String getDatabasePath() {
        return DB_DIR + "/" + DB_NAME;
    }

    public UtilisateurDao getUtilisateurDao() {
        if (utilisateurDao == null) utilisateurDao = new UtilisateurDaoImpl(this);
        return utilisateurDao;
    }

    public AnneeScolaireDao getAnneeScolaireDao() {
        if (anneeScolaireDao == null) anneeScolaireDao = new AnneeScolaireDaoImpl(this);
        return anneeScolaireDao;
    }

    public ClasseDao getClasseDao() {
        if (classeDao == null) classeDao = new ClasseDaoImpl(this);
        return classeDao;
    }

    public EtudiantDao getEtudiantDao() {
        if (etudiantDao == null) etudiantDao = new EtudiantDaoImpl(this);
        return etudiantDao;
    }

    public MatiereDao getMatiereDao() {
        if (matiereDao == null) matiereDao = new MatiereDaoImpl(this);
        return matiereDao;
    }

    public EnseignantDao getEnseignantDao() {
        if (enseignantDao == null) enseignantDao = new EnseignantDaoImpl(this);
        return enseignantDao;
    }

    public EnseignementDao getEnseignementDao() {
        if (enseignementDao == null) enseignementDao = new EnseignementDaoImpl(this);
        return enseignementDao;
    }

    public EvaluationDao getEvaluationDao() {
        if (evaluationDao == null) evaluationDao = new EvaluationDaoImpl(this);
        return evaluationDao;
    }

    public NoteDao getNoteDao() {
        if (noteDao == null) noteDao = new NoteDaoImpl(this);
        return noteDao;
    }
}
