package com.monecole.gestion.dao;

import com.monecole.gestion.models.Note;
import com.monecole.gestion.models.NoteDetail;
import java.util.List;
import java.util.Map;

/**
 * Interface DAO pour la gestion des notes.
 */
public interface NoteDao extends GenericDao<Note, Long> {
    Note findByIdEtudiantAndEvaluation(Long idEtudiant, Long idEvaluation) throws Exception;
    List<Note> findByIdEtudiant(Long idEtudiant) throws Exception;
    List<Note> findByIdEvaluation(Long idEvaluation) throws Exception;
    void batchInsert(List<Note> notes) throws Exception;

    /**
     * Enregistre en une seule transaction les notes d'une évaluation.
     * Les étudiants absents de la table ne sont pas modifiés, les autres sont
     * insérés ou mis à jour selon qu'une note existe déjà.
     *
     * @param idEvaluation      évaluation concernée
     * @param valeursParEtudiant map id_etudiant -> note (valeur nulle ignorée)
     * @return nombre de notes insérées ou mises à jour
     */
    int saveNotesForEvaluation(Long idEvaluation, Map<Long, Double> valeursParEtudiant) throws Exception;

    /**
     * Charge en une seule requête toutes les notes d'une classe, enrichies du
     * contexte d'évaluation, de matière et de coefficient.
     * Utilisé par les moyennes et les statistiques pour éviter les requêtes N+1.
     */
    List<NoteDetail> findByIdClasse(Long idClasse) throws Exception;

    /**
     * Supprime les notes d'une évaluation correspondant à une liste d'étudiants.
     * Utilisé pour effacer une saisie.
     *
     * @return nombre de notes supprimées
     */
    int deleteForEvaluationAndEtudiants(Long idEvaluation, List<Long> idsEtudiants) throws Exception;

    /**
     * Compte les notes saisies pour plusieurs évaluations en une seule requête,
     * pour alimenter une liste d'évaluations sans requête par ligne.
     *
     * @return map id_évaluation -> nombre de notes (absent si zéro)
     */
    Map<Long, Integer> compterNotesParEvaluation(List<Long> idsEvaluations) throws Exception;
}
