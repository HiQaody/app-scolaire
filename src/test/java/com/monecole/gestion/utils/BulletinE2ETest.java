package com.monecole.gestion.utils;

import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.models.*;
import com.monecole.gestion.services.BulletinService;
import com.monecole.gestion.dao.EtudiantDao;
import com.monecole.gestion.dao.ClasseDao;
import com.monecole.gestion.dao.EnseignementDao;
import com.monecole.gestion.dao.EvaluationDao;
import com.monecole.gestion.dao.NoteDao;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Test end-to-end headless du Sprint 5 :
 * 1. Seed la base (données de test standard)
 * 2. Injecte des évaluations et notes d'exemple
 * 3. Génère un bulletin PDF via le vrai BulletinService
 */
public class BulletinE2ETest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Test E2E Bulletin (headless) ===");

        // 1. Seed de la base (crée ~/.ecole-gestion/ecole.db si absente)
        boolean seeded = DatabaseSeeder.seedIfEmpty();
        System.out.println(seeded ? "Base seedée avec données de test." : "Base déjà peuplée.");

        DaoFactory df = DaoFactory.getInstance();
        ClasseDao classeDao = df.getClasseDao();
        EtudiantDao etudiantDao = df.getEtudiantDao();
        EnseignementDao ensDao = df.getEnseignementDao();
        EvaluationDao evalDao = df.getEvaluationDao();
        NoteDao noteDao = df.getNoteDao();

        // 2. Prendre la classe Seconde A et injecter notes si aucune évaluation n'existe
        List<Classe> classes = classeDao.findAll();
        if (classes.isEmpty()) {
            System.out.println("ECHEC: aucune classe en base.");
            System.exit(1);
        }
        Classe secondeA = classes.stream().filter(c -> c.nom().contains("Seconde")).findFirst().orElse(classes.get(0));
        List<Etudiant> etudiants = etudiantDao.findByIdClasse(secondeA.id());
        if (etudiants.isEmpty()) {
            System.out.println("ECHEC: aucun étudiant dans " + secondeA.nom());
            System.exit(1);
        }

        if (evalDao.findAll().isEmpty()) {
            System.out.println("Injection d'évaluations et notes d'exemple...");
            List<Enseignement> ens = ensDao.findByIdClasse(secondeA.id());
            double[][] notesParEval = {
                {12.5, 14.0, 9.5, 16.0, 11.0},  // Devoir 1
                {15.0, 13.5, 12.0, 17.5, 10.5}, // Devoir 2
                {14.0, 15.5, 11.0, 13.0, 12.5}  // Examen
            };
            int evalCount = 0;
            for (Enseignement e : ens) {
                for (int i = 0; i < 3; i++) {
                    String libelle = (i < 2 ? "Devoir " + (i + 1) : "Examen");
                    Evaluation.TypeEvaluation type = (i < 2) ? Evaluation.TypeEvaluation.DEVOIR : Evaluation.TypeEvaluation.EXAMEN;
                    Evaluation ev = evalDao.create(new Evaluation(null, libelle, type,
                        LocalDate.of(2024, new int[]{9, 11, 12}[i], 15), e.id()));
                    evalCount++;
                    List<Etudiant> ets = etudiants;
                    for (int j = 0; j < ets.size() && j < 5; j++) {
                        noteDao.create(new Note(null, notesParEval[i][j], ets.get(j).id(), ev.id()));
                    }
                }
            }
            System.out.println(evalCount + " évaluations créées avec notes pour 5 étudiants.");
        } else {
            System.out.println("Des évaluations existent déjà : injection ignorée.");
        }

        // 3. Générer le bulletin du premier étudiant via le VRAI service
        Etudiant cible = etudiants.get(0);
        System.out.println("Génération du bulletin de : " + cible.prenom() + " " + cible.nom()
            + " (" + cible.matricule() + ")");

        BulletinService service = new BulletinService();
        Path pdf = service.genererBulletin(cible, "Année complète");

        if (Files.exists(pdf) && Files.size(pdf) > 1000) {
            System.out.println("SUCCES E2E : bulletin généré");
            System.out.println("  Fichier : " + pdf.toAbsolutePath());
            System.out.println("  Taille  : " + Files.size(pdf) + " octets");
        } else {
            System.out.println("ECHEC E2E : PDF absent ou trop petit");
            System.exit(1);
        }
    }
}
