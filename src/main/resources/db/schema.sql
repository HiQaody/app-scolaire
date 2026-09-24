-- Schéma de la base de données SQLite (ecole.db)
-- Application de Gestion Scolaire - CDC page 93-106

PRAGMA foreign_keys = ON;

-- 1. Utilisateurs (gestion des droits)
CREATE TABLE IF NOT EXISTS utilisateurs (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    login           TEXT NOT NULL UNIQUE,
    password_hash   TEXT NOT NULL,
    role            TEXT NOT NULL CHECK (role IN ('ADMIN', 'SECRETAIRE', 'ENSEIGNANT')),
    id_enseignant   INTEGER,
    FOREIGN KEY (id_enseignant) REFERENCES enseignants(id) ON DELETE SET NULL
);

-- 2. Années scolaires
CREATE TABLE IF NOT EXISTS annees_scolaires (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    libelle      TEXT NOT NULL UNIQUE,
    est_active   INTEGER NOT NULL DEFAULT 0
);

-- 3. Classes
CREATE TABLE IF NOT EXISTS classes (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    nom       TEXT NOT NULL,
    niveau    TEXT,
    id_annee  INTEGER NOT NULL,
    FOREIGN KEY (id_annee) REFERENCES annees_scolaires(id) ON DELETE CASCADE
);

-- 4. Étudiants
CREATE TABLE IF NOT EXISTS etudiants (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    matricule       TEXT NOT NULL UNIQUE,
    nom             TEXT NOT NULL,
    prenom          TEXT NOT NULL,
    date_naissance  DATE,
    sexe            TEXT CHECK (sexe IN ('M', 'F')),
    id_classe       INTEGER NOT NULL,
    FOREIGN KEY (id_classe) REFERENCES classes(id) ON DELETE CASCADE
);

-- 5. Matières
CREATE TABLE IF NOT EXISTS matieres (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    code          TEXT NOT NULL UNIQUE,
    libelle       TEXT NOT NULL,
    coefficient   REAL NOT NULL DEFAULT 1
);

-- 6. Enseignants
CREATE TABLE IF NOT EXISTS enseignants (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    matricule     TEXT NOT NULL UNIQUE,
    nom           TEXT NOT NULL,
    prenom        TEXT NOT NULL
);

-- 7. Enseignements (table de liaison Enseignant-Matière-Classe)
CREATE TABLE IF NOT EXISTS enseignements (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    id_enseignant  INTEGER NOT NULL,
    id_matiere     INTEGER NOT NULL,
    id_classe      INTEGER NOT NULL,
    FOREIGN KEY (id_enseignant) REFERENCES enseignants(id) ON DELETE CASCADE,
    FOREIGN KEY (id_matiere)   REFERENCES matieres(id) ON DELETE CASCADE,
    FOREIGN KEY (id_classe)    REFERENCES classes(id) ON DELETE CASCADE,
    UNIQUE (id_enseignant, id_matiere, id_classe)
);

-- 8. Évaluations (Devoir 1, Examen, etc.)
CREATE TABLE IF NOT EXISTS evaluations (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    libelle         TEXT NOT NULL,
    type            TEXT NOT NULL CHECK (type IN ('DEVOIR', 'EXAMEN', 'PROJECT', 'PARTICIPATION')),
    date            DATE,
    id_enseignement INTEGER NOT NULL,
    FOREIGN KEY (id_enseignement) REFERENCES enseignements(id) ON DELETE CASCADE
);

-- 9. Notes
CREATE TABLE IF NOT EXISTS notes (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    valeur        REAL,
    id_etudiant   INTEGER NOT NULL,
    id_evaluation INTEGER NOT NULL,
    FOREIGN KEY (id_etudiant)   REFERENCES etudiants(id) ON DELETE CASCADE,
    FOREIGN KEY (id_evaluation) REFERENCES evaluations(id) ON DELETE CASCADE,
    UNIQUE (id_etudiant, id_evaluation)
);
