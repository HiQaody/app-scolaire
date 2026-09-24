# Application de Gestion Scolaire

Application desktop autonome (hors-ligne) de gestion scolaire, développée en Java 21 + Swing + SQLite, conformément au [cahier des charges](cdc.md).

## Stack Technique

| Couche | Technologie |
|--------|------------|
| Langage | Java 21 (Records, Pattern Matching, Switch Expressions) |
| Build | Maven |
| UI | Swing + FlatLaf (Light/Dark) + MigLayout |
| Base de données | SQLite 3 (JDBC pur) |
| Graphiques | JFreeChart |
| Rapports PDF | JasperReports 6.21 (template `src/main/resources/reports/bulletin.jrxml`) |

## Architecture

```
src/main/java/com/monecole/gestion/
├── main/                    # Point d'entrée (MainApp, LoginPanel)
├── models/                  # Entités (Records Java 21)
├── dao/                     # Interfaces + implémentations JDBC (pattern DAO)
│   ├── DaoFactory.java      # Singleton de connexion SQLite
│   ├── GenericDao.java      # Interface CRUD générique
│   ├── AbstractDao.java     # Implémentation abstraite CRUD
│   ├── *Dao.java            # Interfaces spécifiques
│   ├── *DaoImpl.java        # Implémentations JDBC
│   └── DatabaseBackupUtil.java
├── services/                # Logique métier (AuthService)
├── utils/                   # PasswordHasher, DatabaseSeeder
└── views/                   # Interfaces graphiques (Login, MainWindow, CRUD, Notes, Dashboard)
```

## Installation & Exécution

### Prérequis
- JDK 21
- Maven 3.9+

### Build
```bash
mvn clean compile
```

### Exécuter l'application
```bash
mvn exec:java -Dexec.mainClass="com.monecole.gestion.main.MainApp"
```

### Packaging (JAR exécutable)
```bash
mvn clean package
# JAR dans target/gestion-scolaire-1.0.0-shaded.jar
```

## Base de Données

- **Emplacement :** `~/Library/Application Support/.ecole-gestion/ecole.db` (macOS) ou `%USERPROFILE%\.ecole-gestion\ecole.db` (Windows)
- **Initialisation :** Création automatique à la première exécution via `schema.sql`
- **Données de test :** Insérées automatiquement si la base est vide

### Comptes de test
| Login | Mot de passe | Rôle |
|-------|-------------|------|
| `admin` | `admin123` | Administrateur |
| `secretaire` | `secret123` | Secrétaire Pédagogique |
| `prof.math` | `math123` | Enseignant |

## Sauvegarde
```bash
# Via l'interface administrateur (bouton "Sauvegarder")
# Copie ecole.db vers ~/.ecole-gestion/backups/ecole_backup_<timestamp>.db
```

## Fonctionnalités par Sprint

| Sprint | Fonctionnalités | Statut |
|--------|----------------|--------|
| 1 | Config Maven, FlatLaf, BDD SQLite, couche DAO, auth, données test | ✅ Terminé |
| 2 | Interfaces (Login, menu latéral, CRUD Étudiants/Classes) | ✅ Terminé |
| 3 | Module Pédagogique (Matières) + saisie notes (JTable) | ✅ Terminé |
| 4 | Calculs de moyennes + JFreeChart (dashboard) | ✅ Terminé |
| 5 | JasperReports (PDF bulletins) + packaging jpackage | ✅ Terminé (bulletins PDF ; jpackage optionnel à venir) |
