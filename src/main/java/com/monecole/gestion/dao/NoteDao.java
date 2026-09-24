package com.monecole.gestion.dao;

import com.monecole.gestion.models.Note;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface DAO pour la gestion des notes.
 */
public interface NoteDao extends GenericDao<Note, Long> {
    Note findByIdEtudiantAndEvaluation(Long idEtudiant, Long idEvaluation) throws Exception;
    List<Note> findByIdEtudiant(Long idEtudiant) throws Exception;
    List<Note> findByIdEvaluation(Long idEvaluation) throws Exception;
    void batchInsert(List<Note> notes) throws Exception;
}
