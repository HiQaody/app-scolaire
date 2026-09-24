
# 📄 CAHIER DES CHARGES : Application de Gestion Scolaire (Desktop)

## 1. Présentation du Projet
### 1.1. Contexte
L'établissement souhaite se doter d'un outil informatique local (desktop) pour remplacer la gestion papier/Excel des notes et des matières. L'application doit être autonome, rapide, sécurisée et capable de fonctionner sans connexion internet.

### 1.2. Objectifs Principaux
*   Centraliser les données des étudiants, des classes, des matières et des enseignants.
*   Automatiser le calcul des moyennes et des rangs.
*   Offrir une analyse visuelle des résultats (statistiques).
*   Générer des bulletins de notes professionnels au format PDF.

---

## 2. Spécifications Fonctionnelles
### 2.1. Acteurs et Rôles (Gestion des droits)
*   **Administrateur :** Accès total. Configure l'année scolaire, gère les utilisateurs, sauvegarde la base de données.
*   **Secrétaire Pédagogique :** Gère les étudiants, les classes, les matières, et édite les bulletins.
*   **Enseignant :** Saisie les notes uniquement pour les matières qui lui sont attribuées et consulte les statistiques de ses classes.

### 2.2. Modules Fonctionnels
1.  **Module Authentification :** Écran de login sécurisé avec hashage des mots de passe.
2.  **Module Scolarité :** CRUD (Créer, Lire, Mettre à jour, Supprimer) pour les Étudiants, Classes, Filières et Enseignants.
3.  **Module Pédagogique :** Définition des matières, affectation des coefficients, et liaison Enseignant-Matière-Classe.
4.  **Module Notes et Évaluations :**
    *   Saisie des notes par devoir/examen.
    *   Validation et verrouillage des notes après correction.
5.  **Module Calculs et Bulletins :** Calcul des moyennes (matière, trimestre, annuelle) et génération PDF.
6.  **Module Statistiques (Dashboard) :** Affichage des taux de réussite, moyennes de classe, etc.

---

## 3. Spécifications Techniques (Le Stack)

### 3.1. Environnement de Développement
*   **Langage :** **Java 21** (Utilisation des fonctionnalités modernes : *Records* pour les DTO, *Pattern Matching*, *Switch Expressions*).
*   **IDE :** **Apache NetBeans** (Version 21 ou supérieure). Utilisation intensive du **GUI Builder (Matisse)** pour le design des interfaces Swing.
*   **Gestionnaire de dépendances :** **Maven** (Fichier `pom.xml` pour gérer les librairies).

### 3.2. Architecture et Frameworks
*   **Interface Graphique (UI) :** **Java Swing** + **FlatLaf** (Flat Look and Feel).
    *   *Configuration :* Application du thème `FlatLightLaf` ou `FlatDarkLaf` au démarrage pour un design moderne.
    *   *Layout :* Utilisation de `MigLayout` (via Maven) ou du `GroupLayout` natif de NetBeans pour des interfaces responsives.
*   **Base de Données :** **SQLite** (Version 3.x).
    *   *Avantage :* Base de données contenue dans un simple fichier `.db` local. Zéro configuration serveur.
    *   *Accès :* **JDBC** pur avec implémentation du pattern **DAO** (Data Access Object) pour séparer le code SQL de la logique métier.
*   **Graphiques et Statistiques :** **JFreeChart**.
    *   Intégration de graphiques en camembert (répartition des mentions) et histogrammes (évolution des moyennes) directement dans des `JPanel` Swing.
*   **Édition de Rapports (PDF) :** **JasperReports**.
    *   Conception des templates de bulletins via **Jaspersoft Studio** (fichiers `.jrxml`).
    *   Compilation et remplissage des rapports via le moteur JasperReports dans le code Java.

### 3.3. Dépendances Maven (`pom.xml`)
Voici les dépendances clés à inclure dans le projet NetBeans :
```xml
<dependencies>
    <!-- FlatLaf pour le design moderne -->
    <dependency>
        <groupId>com.formdev</groupId>
        <artifactId>flatlaf</artifactId>
        <version>3.4</version>
    </dependency>
    <!-- SQLite JDBC Driver -->
    <dependency>
        <groupId>org.xerial</groupId>
        <artifactId>sqlite-jdbc</artifactId>
        <version>3.45.1.0</version>
    </dependency>
    <!-- JFreeChart pour les graphiques -->
    <dependency>
        <groupId>org.jfree</groupId>
        <artifactId>jfreechart</artifactId>
        <version>1.5.4</version>
    </dependency>
    <!-- JasperReports pour les PDF -->
    <dependency>
        <groupId>net.sf.jasperreports</groupId>
        <artifactId>jasperreports</artifactId>
        <version>6.21.0</version>
    </dependency>
    <!-- MigLayout (Optionnel mais recommandé pour un meilleur layout Swing) -->
    <dependency>
        <groupId>com.miglayout</groupId>
        <artifactId>miglayout-swing</artifactId>
        <version>11.3</version>
    </dependency>
</dependencies>
```

---

## 4. Modèle de Données (Schéma SQLite)

La base de données SQLite (`ecole.db`) sera structurée avec les tables suivantes :

1.  **`utilisateurs`** (id, login, password_hash, role, id_enseignant)
2.  **`annees_scolaires`** (id, libelle, est_active)
3.  **`classes`** (id, nom, niveau, id_annee)
4.  **`etudiants`** (id, matricule, nom, prenom, date_naissance, sexe, id_classe)
5.  **`matieres`** (id, code, libelle, coefficient)
6.  **`enseignants`** (id, matricule, nom, prenom)
7.  **`enseignements`** (id, id_enseignant, id_matiere, id_classe) *Table de liaison*
8.  **`evaluations`** (id, libelle, type, date, id_enseignement) *Ex: Devoir 1, Examen*
9.  **`notes`** (id, valeur, id_etudiant, id_evaluation)

---

## 5. Ergonomie et Design (UI/UX)

### 5.1. Charte Graphique (FlatLaf)
*   Utilisation des couleurs par défaut de FlatLaf (Bleu primaire pour les actions, Rouge pour les suppressions, Vert pour les validations).
*   Support du **Mode Sombre (Dark Mode)** et **Mode Clair (Light Mode)** via un bouton de bascule dans la barre de navigation.
*   Icônes vectorielles (SVG ou PNG haute résolution) intégrées via la classe `UIManager` de FlatLaf.

### 5.2. Navigation et Interfaces
*   **Fenêtre Principale (`JFrame`) :**
    *   *Gauche :* Menu latéral (Sidebar) avec navigation par modules (Dashboard, Étudiants, Notes, Bulletins).
    *   *Centre :* Zone de travail dynamique (`JPanel` avec `CardLayout` pour changer de vue sans rouvrir de fenêtres).
    *   *Haut :* Barre de titre personnalisée avec le profil utilisateur et le bouton de déconnexion.
*   **Formulaires :** Utilisation de `JInternalFrame` ou de `JDialog` modaux pour les ajouts/modifications.

---

## 6. Architecture Logicielle (Code Java)

Le code source dans NetBeans sera organisé selon une architecture en couches stricte :

```text
src/main/java/com/monécole/gestion/
│
├── main/                 # Point d'entrée (Initialisation FlatLaf, Login)
├── models/               # Entités (Etudiant, Note, etc.) et Records Java 21
├── dao/                  # Interfaces et Implémentations JDBC (SQLite)
│   ├── DaoFactory.java   # Gestion du singleton Connection SQLite
│   ├── EtudiantDao.java
│   └── NoteDao.java
├── services/             # Logique métier (Calcul Moyennes, Vérifications)
├── controllers/          # Contrôleurs Swing (ActionListeners, liaison Vue/Service)
├── views/                # Interfaces Graphiques (JFrame, JPanel générés via NetBeans)
│   ├── dashboard/
│   ├── students/
│   └── grades/
└── utils/                # Classes utilitaires (Export PDF Jasper, Chart Factory)
```

---

## 7. Livrables Attendus

À la fin du développement, le projet devra livrer :

1.  **Code Source Complet :** Projet Maven NetBeans fonctionnel, propre et commenté.
2.  **Base de Données :** Fichier `ecole.db` SQLite initialisé avec des données de test.
3.  **Templates Rapports :** Fichiers `.jrxml` (JasperReports) pour le bulletin de notes.
4.  **Exécutable (Packaging) :**
    *   Un fichier `.jar` "Fat" (contenant toutes les dépendances Maven via le plugin `maven-shade-plugin` ou `maven-assembly-plugin`).
    *   *Optionnel :* Un installeur Windows/Mac généré via **jpackage** (inclus dans le JDK 21) qui embarque le JRE pour que l'utilisateur n'ait pas besoin d'installer Java.
5.  **Documentation :**
    *   Manuel d'utilisation (PDF).
    *   Documentation technique (Comment configurer NetBeans, comment modifier le template Jasper).

---

## 8. Contraintes et Règles de Qualité

*   **Fonctionnement Hors-Ligne :** L'application ne doit faire aucun appel réseau. SQLite garantit cela.
*   **Intégrité des données :** Utilisation des transactions JDBC (`Connection.commit()` / `rollback()`) lors de la saisie des notes pour éviter les corruptions en cas de crash.
*   **Sauvegarde :** Le module Administrateur doit inclure un bouton "Sauvegarder" qui copie simplement le fichier `ecole.db` vers un répertoire de backup avec un timestamp.
*   **Performance :** Les `JTable` affichant les notes doivent utiliser le *Lazy Loading* ou une pagination si la classe dépasse 100 étudiants, pour ne pas bloquer l'Event Dispatch Thread (EDT) de Swing. Utilisation de `SwingWorker` pour les calculs lourds et la génération PDF.

---

## 9. Méthodologie de Développement (Sprint suggéré)

*   **Sprint 1 :** Configuration du projet Maven dans NetBeans, intégration de FlatLaf, création de la BDD SQLite et de la couche DAO.
*   **Sprint 2 :** Développement des interfaces de base (Login, Menu principal) et du module Scolarité (CRUD Étudiants/Classes).
*   **Sprint 3 :** Développement du module Pédagogique (Matières) et du module de Saisie des Notes (JTable complexes).
*   **Sprint 4 :** Implémentation de la logique métier (Calculs de moyennes) et intégration de JFreeChart pour le Dashboard.
*   **Sprint 5 :** Intégration de JasperReports, design du bulletin PDF, tests finaux et packaging (jpackage).