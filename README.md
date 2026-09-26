# Application de Gestion Scolaire

Application desktop autonome (hors-ligne) de gestion scolaire, développée en Java 21 + Swing + SQLite, conformément au [cahier des charges](cdc.md).

## Stack Technique

| Couche | Technologie |
|--------|------------|
| Langage | Java 21 (Records, Pattern Matching, Switch Expressions) |
| Build | Maven |
| UI | Swing + FlatLaf (Light/Dark) + MigLayout |
| Base de données | SQLite 3 (JDBC pur) |
| Graphiques | Custom Java 2D (Graphics2D) — BarChartPanel, PieChartPanel |
| Rapports PDF | JasperReports 6.21 (template `src/main/resources/reports/bulletin.jrxml`) |

## Architecture

```
src/main/java/com/monecole/gestion/
├── main/                    # Point d'entrée (MainApp, LoginPanel)
├── controllers/             # Contrôleurs Swing (ActionListeners, liaison Vue/Service)
├── models/                  # Entités (Records Java 21)
│   ├── AnneeScolaire.java
│   ├── Classe.java
│   ├── Enseignant.java
│   ├── Enseignement.java
│   ├── Etudiant.java
│   ├── Evaluation.java
│   ├── Matiere.java
│   ├── Note.java
│   ├── NoteDetail.java
│   └── Utilisateur.java
├── dao/                     # Interfaces + implémentations JDBC (pattern DAO)
│   ├── DaoFactory.java      # Singleton de connexion SQLite
│   ├── GenericDao.java      # Interface CRUD générique
│   ├── AbstractDao.java     # Implémentation abstraite CRUD
│   ├── *Dao.java            # Interfaces spécifiques
│   ├── *DaoImpl.java        # Implémentations JDBC
│   └── DatabaseBackupUtil.java
├── services/                # Logique métier
│   ├── AuthService.java
│   ├── BulletinService.java
│   ├── EtudiantService.java
│   ├── MoyenneService.java
│   └── NoteService.java
├── utils/                   # Utilitaires
│   ├── DatabaseMigration.java
│   ├── DatabaseSeeder.java
│   └── PasswordHasher.java
└── views/                   # Interfaces graphiques
    ├── MainWindow.java
    ├── classes/             # Gestion des classes
    ├── dashboard/           # Tableau de bord (graphiques 2D custom)
    ├── enseignants/         # Gestion des enseignants
    ├── enseignements/       # Gestion des enseignements
    ├── grades/              # Notes, évaluations, statistiques, synthèse
    ├── matieres/            # Gestion des matières
    ├── reports/             # Visualisation des bulletins PDF
    └── students/            # Gestion des étudiants
src/test/java/               # Tests E2E
└── BulletinE2ETest.java     # Test end-to-end de génération de bulletins
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
- **Migrations :** Appliquées automatiquement par `DatabaseMigration` (ajout de colonnes `poids`, `bareme` sur `evaluations`)
- **Données de test :** Insérées automatiquement si la base est vide via `DatabaseSeeder`

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

## Tests

Un test E2E headless est disponible pour valider la génération de bulletins PDF :
```bash
# Exécuter le test E2E
java -cp "target/gestion-scolaire-1.0.0-shaded.jar" com.monecole.gestion.utils.BulletinE2ETest
```

## Fonctionnalités par Sprint

| Sprint | Fonctionnalités | Statut |
|--------|----------------|--------|
| 1 | Config Maven, FlatLaf, BDD SQLite, couche DAO, auth, données test | ✅ Terminé |
| 2 | Interfaces (Login, menu latéral, CRUD Étudiants/Classes) | ✅ Terminé |
| 3 | Module Pédagogique (Matières) + saisie notes (JTable) | ✅ Terminé |
| 4 | Calculs de moyennes + graphiques 2D custom (dashboard) | ✅ Terminé |
| 5 | JasperReports (PDF bulletins) + tests E2E + packaging jpackage | ✅ Terminé (bulletins PDF, tests E2E ; jpackage optionnel à venir) |
