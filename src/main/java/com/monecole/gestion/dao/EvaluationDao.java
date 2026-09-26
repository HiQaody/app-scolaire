package com.monecole.gestion.dao;

import com.monecole.gestion.models.Evaluation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface DAO pour la gestion des évaluations.
 */
public interface EvaluationDao extends GenericDao<Evaluation, Long> {
    List<Evaluation> findByIdEnseignement(Long idEnseignement) throws Exception;
    List<Evaluation> findByIdClasseAndMatiere(Long idClasse, Long idMatiere) throws Exception;

    /** Toutes les évaluations d'une classe, tous enseignements confondus. */
    List<Evaluation> findByIdClasse(Long idClasse) throws Exception;

    /** Toutes les évaluations d'un enseignant, de la plus récente à la plus ancienne. */
    List<Evaluation> findByIdEnseignant(Long idEnseignant) throws Exception;
}
