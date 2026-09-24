package com.monecole.gestion.utils;

import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.models.*;

import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilitaire pour insérer des données de test lors de la première exécution.
 */
public class DatabaseSeeder {

    private static final Logger LOGGER = Logger.getLogger(DatabaseSeeder.class.getName());
    public DatabaseSeeder() {
        DaoFactory.getInstance();
    }

    /**
     * Insère des données de test si la base est vide.
     * @return true si des données ont été insérées, false si la base contenait déjà des données
     */
    public static boolean seedIfEmpty() {
        try {
            DaoFactory df = DaoFactory.getInstance();
            if (df.getEtudiantDao().count() > 0) {
                return false;
            }
            new DatabaseSeeder().seed(df);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'insertion des données de test", e);
            return false;
        }
    }

    /**
     * Insère des années scolaires, classes, matières, enseignants, étudiants, enseignements et utilisateurs.
     */
    private void seed(DaoFactory df) throws Exception {
        // Années scolaires
        AnneeScolaire annee = df.getAnneeScolaireDao().create(new AnneeScolaire(null, "2024-2025", true));

        // Classes
        Classe seconde = df.getClasseDao().create(new Classe(null, "Seconde A", "Seconde", annee.id()));
        Classe premiere = df.getClasseDao().create(new Classe(null, "Première B", "Première", annee.id()));
        Classe terminale = df.getClasseDao().create(new Classe(null, "Terminale C", "Terminale", annee.id()));

        // Matières
        Matiere math = df.getMatiereDao().create(new Matiere(null, "MAT001", "Mathématiques", 4.0));
        Matiere francais = df.getMatiereDao().create(new Matiere(null, "MAT002", "Français", 2.0));
        Matiere physique = df.getMatiereDao().create(new Matiere(null, "MAT003", "Physique-Chimie", 3.0));
        Matiere svt = df.getMatiereDao().create(new Matiere(null, "MAT004", "SVT", 2.0));

        // Enseignants
        Enseignant profMath = df.getEnseignantDao().create(new Enseignant(null, "ENS001", "Dupont", "Jean"));
        Enseignant profFr = df.getEnseignantDao().create(new Enseignant(null, "ENS002", "Martin", "Sophie"));
        Enseignant profPhys = df.getEnseignantDao().create(new Enseignant(null, "ENS003", "Bernard", "Pierre"));
        Enseignant profSvt = df.getEnseignantDao().create(new Enseignant(null, "ENS004", "Durand", "Isabelle"));

        // Enseignements (liaison Enseignant-Matière-Classe)
        df.getEnseignementDao().create(new Enseignement(null, profMath.id(), math.id(), seconde.id()));
        df.getEnseignementDao().create(new Enseignement(null, profFr.id(), francais.id(), seconde.id()));
        df.getEnseignementDao().create(new Enseignement(null, profPhys.id(), physique.id(), seconde.id()));
        df.getEnseignementDao().create(new Enseignement(null, profMath.id(), math.id(), premiere.id()));
        df.getEnseignementDao().create(new Enseignement(null, profFr.id(), francais.id(), premiere.id()));
        df.getEnseignementDao().create(new Enseignement(null, profSvt.id(), svt.id(), terminale.id()));
        df.getEnseignementDao().create(new Enseignement(null, profMath.id(), math.id(), terminale.id()));

        // Étudiants (Seconde A)
        df.getEtudiantDao().create(new Etudiant(null, "ETU001", "Alvarez", "Lucas", LocalDate.of(2007, 3, 15), Etudiant.Sexe.M, seconde.id()));
        df.getEtudiantDao().create(new Etudiant(null, "ETU002", "Garcia", "Emma", LocalDate.of(2007, 7, 22), Etudiant.Sexe.F, seconde.id()));
        df.getEtudiantDao().create(new Etudiant(null, "ETU003", "Benali", "Karim", LocalDate.of(2007, 11, 5), Etudiant.Sexe.M, seconde.id()));
        df.getEtudiantDao().create(new Etudiant(null, "ETU004", "Nguyen", "Linh", LocalDate.of(2007, 1, 30), Etudiant.Sexe.F, seconde.id()));
        df.getEtudiantDao().create(new Etudiant(null, "ETU005", "Oumar", "Bakary", LocalDate.of(2007, 9, 12), Etudiant.Sexe.M, seconde.id()));

        // Étudiants (Première B)
        df.getEtudiantDao().create(new Etudiant(null, "ETU006", "Thomas", "Paul", LocalDate.of(2006, 4, 18), Etudiant.Sexe.M, premiere.id()));
        df.getEtudiantDao().create(new Etudiant(null, "ETU007", "Robert", "Claire", LocalDate.of(2006, 8, 3), Etudiant.Sexe.F, premiere.id()));
        df.getEtudiantDao().create(new Etudiant(null, "ETU008", "Kone", "Moussa", LocalDate.of(2006, 12, 25), Etudiant.Sexe.M, premiere.id()));

        // Étudiants (Terminale C)
        df.getEtudiantDao().create(new Etudiant(null, "ETU009", "Lemoine", "Arthur", LocalDate.of(2005, 2, 14), Etudiant.Sexe.M, terminale.id()));
        df.getEtudiantDao().create(new Etudiant(null, "ETU010", "Fabre", "Juliette", LocalDate.of(2005, 6, 7), Etudiant.Sexe.F, terminale.id()));

        // Utilisateurs (comptes de test)
        Utilisateur admin = new Utilisateur(null, "admin", PasswordHasher.hash("admin123"), Utilisateur.Role.ADMIN, null);
        df.getUtilisateurDao().create(admin);

        Utilisateur secretaire = new Utilisateur(null, "secretaire", PasswordHasher.hash("secret123"), Utilisateur.Role.SECRETAIRE, null);
        df.getUtilisateurDao().create(secretaire);

        // Comptes enseignants
        Utilisateur profUser = new Utilisateur(null, "prof.math", PasswordHasher.hash("math123"), Utilisateur.Role.ENSEIGNANT, profMath.id());
        df.getUtilisateurDao().create(profUser);

        LOGGER.info("Données de test insérées avec succès.");
    }
}
