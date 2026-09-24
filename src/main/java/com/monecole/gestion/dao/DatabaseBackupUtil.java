package com.monecole.gestion.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Utilitaire pour la sauvegarde de la base de données.
 * Copie le fichier ecole.db vers un répertoire de backup avec un timestamp.
 */
public class DatabaseBackupUtil {

    private static final Logger LOGGER = Logger.getLogger(DatabaseBackupUtil.class.getName());
    private static final String DB_DIR = System.getProperty("user.home") + "/.ecole-gestion";
    private static final String DB_NAME = "ecole.db";
    private static final String BACKUP_DIR_NAME = "backups";

    private DatabaseBackupUtil() {}

    /**
     * Crée une sauvegarde de la base de données avec un timestamp.
     * @return le chemin du fichier de sauvegarde
     */
    public static String backupDatabase() throws IOException {
        Path dbPath = Paths.get(DB_DIR, DB_NAME);
        if (!Files.exists(dbPath)) {
            throw new IOException("Base de données non trouvée : " + dbPath);
        }

        Path backupDir = Paths.get(DB_DIR, BACKUP_DIR_NAME);
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path backupFile = backupDir.resolve("ecole_backup_" + timestamp + ".db");

        Files.copy(dbPath, backupFile, StandardCopyOption.REPLACE_EXISTING);

        LOGGER.info("Sauvegarde créée : " + backupFile);
        return backupFile.toString();
    }
}
