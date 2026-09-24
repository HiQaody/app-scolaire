package com.monecole.gestion.dao;

import com.monecole.gestion.models.Matiere;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface DAO pour la gestion des matières.
 */
public interface MatiereDao extends GenericDao<Matiere, Long> {
    Matiere findByCode(String code) throws Exception;
}
