package com.monecole.gestion.dao;

import com.monecole.gestion.models.Enseignement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface DAO pour la gestion des enseignements (liaison Enseignant-Matière-Classe).
 */
public interface EnseignementDao extends GenericDao<Enseignement, Long> {
    Enseignement findByIds(Long idEnseignant, Long idMatiere, Long idClasse) throws Exception;
    List<Enseignement> findByIdEnseignant(Long idEnseignant) throws Exception;
    List<Enseignement> findByIdClasse(Long idClasse) throws Exception;
}
