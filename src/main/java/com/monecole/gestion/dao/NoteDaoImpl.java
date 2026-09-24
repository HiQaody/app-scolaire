package com.monecole.gestion.dao;

import com.monecole.gestion.models.Note;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation JDBC du DAO Note.
 * Utilise des transactions pour garantir l'intégrité des données.
 */
public class NoteDaoImpl extends AbstractDao<Note, Long> implements NoteDao {

    public NoteDaoImpl(DaoFactory daoFactory) {
        super(daoFactory);
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO notes (valeur, id_etudiant, id_evaluation) VALUES (?, ?, ?)";
    }

    @Override
    protected void setInsertParams(PreparedStatement ps, Note n) throws Exception {
        ps.setObject(1, n.valeur());
        ps.setLong(2, n.idEtudiant());
        ps.setLong(3, n.idEvaluation());
    }

    @Override
    protected String getFindByIdSql() {
        return "SELECT id, valeur, id_etudiant, id_evaluation FROM notes WHERE id = ?";
    }

    @Override
    protected String getFindAllSql() {
        return "SELECT id, valeur, id_etudiant, id_evaluation FROM notes";
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE notes SET valeur = ?, id_etudiant = ?, id_evaluation = ? WHERE id = ?";
    }

    @Override
    protected void setUpdateParams(PreparedStatement ps, Note n) throws Exception {
        ps.setObject(1, n.valeur());
        ps.setLong(2, n.idEtudiant());
        ps.setLong(3, n.idEvaluation());
        ps.setLong(4, n.id());
    }

    @Override
    protected String getDeleteSql() {
        return "DELETE FROM notes WHERE id = ?";
    }

    @Override
    protected String getTableName() {
        return "notes";
    }

    @Override
    protected Note copyWithId(Note n, Long id) {
        return new Note(id, n.valeur(), n.idEtudiant(), n.idEvaluation());
    }

    @Override
    protected Note mapRow(ResultSet rs) throws Exception {
        return new Note(
            rs.getLong("id"),
            rs.getObject("valeur") != null ? rs.getDouble("valeur") : null,
            rs.getLong("id_etudiant"),
            rs.getLong("id_evaluation")
        );
    }

    @Override
    public Note findByIdEtudiantAndEvaluation(Long idEtudiant, Long idEvaluation) throws Exception {
        String sql = "SELECT id, valeur, id_etudiant, id_evaluation FROM notes WHERE id_etudiant = ? AND id_evaluation = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, idEtudiant);
            ps.setLong(2, idEvaluation);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public List<Note> findByIdEtudiant(Long idEtudiant) throws Exception {
        String sql = "SELECT id, valeur, id_etudiant, id_evaluation FROM notes WHERE id_etudiant = ? ORDER BY id_evaluation";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, idEtudiant);
            try (ResultSet rs = ps.executeQuery()) {
                return extractList(rs);
            }
        }
    }

    @Override
    public List<Note> findByIdEvaluation(Long idEvaluation) throws Exception {
        String sql = "SELECT id, valeur, id_etudiant, id_evaluation FROM notes WHERE id_evaluation = ? ORDER BY id_etudiant";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, idEvaluation);
            try (ResultSet rs = ps.executeQuery()) {
                return extractList(rs);
            }
        }
    }

    @Override
    public void batchInsert(List<Note> notes) throws Exception {
        String sql = "INSERT OR REPLACE INTO notes (valeur, id_etudiant, id_evaluation) VALUES (?, ?, ?)";
        Connection conn = getConnection();
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (Note n : notes) {
                ps.setObject(1, n.valeur());
                ps.setLong(2, n.idEtudiant());
                ps.setLong(3, n.idEvaluation());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private List<Note> extractList(ResultSet rs) throws Exception {
        List<Note> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }
}
