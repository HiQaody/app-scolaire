package com.monecole.gestion.dao;

import com.monecole.gestion.models.AnneeScolaire;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface DAO pour la gestion des années scolaires.
 */
public interface AnneeScolaireDao extends GenericDao<AnneeScolaire, Long> {
    AnneeScolaire findActive() throws Exception;
    void setActive(Long id) throws Exception;
}
