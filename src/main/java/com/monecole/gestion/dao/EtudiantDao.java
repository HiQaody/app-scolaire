package com.monecole.gestion.dao;

import com.monecole.gestion.models.Etudiant;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface DAO pour la gestion des étudiants.
 */
public interface EtudiantDao extends GenericDao<Etudiant, Long> {
    List<Etudiant> findByIdClasse(Long idClasse) throws Exception;
    Etudiant findByMatricule(String matricule) throws Exception;
}
