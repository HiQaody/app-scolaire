package com.monecole.gestion.dao;

import com.monecole.gestion.models.Enseignant;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface DAO pour la gestion des enseignants.
 */
public interface EnseignantDao extends GenericDao<Enseignant, Long> {
    Enseignant findByMatricule(String matricule) throws Exception;
}
