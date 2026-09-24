package com.monecole.gestion.dao;

import com.monecole.gestion.models.Classe;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface DAO pour la gestion des classes.
 */
public interface ClasseDao extends GenericDao<Classe, Long> {
    List<Classe> findByIdAnnee(Long idAnnee) throws Exception;
}
