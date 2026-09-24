package com.monecole.gestion.services;

import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.dao.EtudiantDao;
import com.monecole.gestion.models.Etudiant;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Service métier pour la gestion des étudiants.
 */
public class EtudiantService {

    private static final Logger LOGGER = Logger.getLogger(EtudiantService.class.getName());
    private static final DateTimeFormatter MATRICULE_FMT = DateTimeFormatter.ofPattern("yyyyMM");
    private final EtudiantDao etudiantDao;
    private final AtomicLong matriculeCounter = new AtomicLong(0);

    public EtudiantService() {
        this.etudiantDao = DaoFactory.getInstance().getEtudiantDao();
    }

    /**
     * Crée un nouvel étudiant en générant automatiquement un matricule unique.
     */
    public Etudiant createEtudiant(String nom, String prenom, LocalDate dateNaissance,
                                   Etudiant.Sexe sexe, Long idClasse) throws Exception {
        String matricule = generateMatricule(nom, prenom);
        Etudiant etudiant = new Etudiant(null, matricule, nom, prenom, dateNaissance, sexe, idClasse);
        return etudiantDao.create(etudiant);
    }

    /**
     * Génère un matricule unique basé sur le nom, prénom et un compteur.
     */
    private String generateMatricule(String nom, String prenom) {
        String initials = (nom.toUpperCase().charAt(0) + "" + prenom.toUpperCase().charAt(0)).toUpperCase();
        long count = matriculeCounter.incrementAndGet();
        String year = LocalDate.now().format(MATRICULE_FMT);
        return "ETU" + year + "-" + initials + String.format("%03d", count);
    }

    /**
     * Met à jour un étudiant existant.
     */
    public void updateEtudiant(Long id, String matricule, String nom, String prenom,
                               LocalDate dateNaissance, Etudiant.Sexe sexe, Long idClasse) throws Exception {
        Etudiant etudiant = new Etudiant(id, matricule, nom, prenom, dateNaissance, sexe, idClasse);
        etudiantDao.update(etudiant);
    }

    /**
     * Supprime un étudiant.
     */
    public void deleteEtudiant(Long id) throws Exception {
        etudiantDao.delete(id);
    }

    /**
     * Retourne tous les étudiants d'une classe.
     */
    public List<Etudiant> findByClasse(Long idClasse) throws Exception {
        return etudiantDao.findByIdClasse(idClasse);
    }

    /**
     * Recherche un étudiant par matricule.
     */
    public Etudiant findByMatricule(String matricule) throws Exception {
        return etudiantDao.findByMatricule(matricule);
    }
}
