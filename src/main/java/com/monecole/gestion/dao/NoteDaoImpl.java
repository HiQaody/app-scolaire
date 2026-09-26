package com.monecole.gestion.dao;

import com.monecole.gestion.models.Evaluation;
import com.monecole.gestion.models.Note;
import com.monecole.gestion.models.NoteDetail;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implémentation JDBC du DAO Note.
 * Utilise des transactions pour garantir l'intégrité des données.
 */
public class NoteDaoImpl extends AbstractDao<Note, Long> implements NoteDao {

    /**
     * Sérialise les transactions : la connexion SQLite est partagée par tous les DAO,
     * une seule transaction peut donc être active à la fois.
     */
    private static final Object TX_LOCK = new Object();

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
        synchronized (TX_LOCK) {
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
    }

    @Override
    public int saveNotesForEvaluation(Long idEvaluation, Map<Long, Double> valeursParEtudiant) throws Exception {
        if (idEvaluation == null || valeursParEtudiant.isEmpty()) {
            return 0;
        }
        Connection conn = getConnection();
        synchronized (TX_LOCK) {
            conn.setAutoCommit(false);
            try {
                Map<Long, Long> idsExistants = findExistingNoteIds(conn, idEvaluation);
                try (PreparedStatement updatePs = conn.prepareStatement(
                        "UPDATE notes SET valeur = ? WHERE id = ?")) {
                    try (PreparedStatement insertPs = conn.prepareStatement(
                            "INSERT INTO notes (valeur, id_etudiant, id_evaluation) VALUES (?, ?, ?)")) {
                        for (Map.Entry<Long, Double> entry : valeursParEtudiant.entrySet()) {
                            if (entry.getKey() == null || entry.getValue() == null) {
                                continue;
                            }
                            Long noteId = idsExistants.get(entry.getKey());
                            if (noteId != null) {
                                updatePs.setDouble(1, entry.getValue());
                                updatePs.setLong(2, noteId);
                                updatePs.addBatch();
                            } else {
                                insertPs.setDouble(1, entry.getValue());
                                insertPs.setLong(2, entry.getKey());
                                insertPs.setLong(3, idEvaluation);
                                insertPs.addBatch();
                            }
                        }
                        updatePs.executeBatch();
                        insertPs.executeBatch();
                    }
                }
                conn.commit();
                return valeursParEtudiant.size();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** Charge les ids de notes d'une évaluation indexés par id_etudiant. */
    private Map<Long, Long> findExistingNoteIds(Connection conn, Long idEvaluation) throws Exception {
        String sql = "SELECT id, id_etudiant FROM notes WHERE id_evaluation = ?";
        Map<Long, Long> ids = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idEvaluation);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.put(rs.getLong("id_etudiant"), rs.getLong("id"));
                }
            }
        }
        return ids;
    }

    @Override
    public List<NoteDetail> findByIdClasse(Long idClasse) throws Exception {
        String sql = """
            SELECT n.id, n.valeur, n.id_etudiant, n.id_evaluation,
                   e.id AS id_enseignement, e.id_matiere, e.id_classe,
                   m.code, m.libelle AS matiere_libelle, m.coefficient,
                   ev.libelle AS evaluation_libelle, ev.type, ev.date, ev.poids, ev.bareme
            FROM notes n
            JOIN evaluations ev ON n.id_evaluation = ev.id
            JOIN enseignements e ON ev.id_enseignement = e.id
            JOIN matieres m ON e.id_matiere = m.id
            WHERE e.id_classe = ?
            ORDER BY n.id_etudiant, ev.date, ev.id
            """;
        List<NoteDetail> details = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, idClasse);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object valeur = rs.getObject("valeur");
                    details.add(new NoteDetail(
                        rs.getLong("id"),
                        valeur == null ? null : ((Number) valeur).doubleValue(),
                        rs.getLong("id_etudiant"),
                        rs.getLong("id_evaluation"),
                        rs.getLong("id_enseignement"),
                        rs.getLong("id_matiere"),
                        rs.getLong("id_classe"),
                        rs.getString("code"),
                        rs.getString("matiere_libelle"),
                        rs.getDouble("coefficient"),
                        rs.getString("evaluation_libelle"),
                        Evaluation.TypeEvaluation.fromValue(rs.getString("type")),
                        rs.getObject("date") != null ? rs.getObject("date", LocalDate.class) : null,
                        rs.getDouble("poids"),
                        rs.getDouble("bareme")
                    ));
                }
            }
        }
        return details;
    }

    @Override
    public int deleteForEvaluationAndEtudiants(Long idEvaluation, List<Long> idsEtudiants) throws Exception {
        if (idEvaluation == null || idsEtudiants == null || idsEtudiants.isEmpty()) {
            return 0;
        }
        String sql = "DELETE FROM notes WHERE id_evaluation = ? AND id_etudiant = ?";
        Connection conn = getConnection();
        synchronized (TX_LOCK) {
            int total = 0;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Long idEtudiant : idsEtudiants) {
                    ps.setLong(1, idEvaluation);
                    ps.setLong(2, idEtudiant);
                    total += ps.executeUpdate();
                }
            }
            return total;
        }
    }

    @Override
    public Map<Long, Integer> compterNotesParEvaluation(List<Long> idsEvaluations) throws Exception {
        Map<Long, Integer> resultat = new HashMap<>();
        if (idsEvaluations == null || idsEvaluations.isEmpty()) {
            return resultat;
        }
        StringBuilder sql = new StringBuilder(
            "SELECT id_evaluation, COUNT(*) AS nb FROM notes WHERE id_evaluation IN (");
        for (int i = 0; i < idsEvaluations.size(); i++) {
            sql.append(i == 0 ? "?" : ",?");
        }
        sql.append(") GROUP BY id_evaluation");

        try (PreparedStatement ps = getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < idsEvaluations.size(); i++) {
                ps.setLong(i + 1, idsEvaluations.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultat.put(rs.getLong("id_evaluation"), rs.getInt("nb"));
                }
            }
        }
        return resultat;
    }

    private List<Note> extractList(ResultSet rs) throws Exception {
        List<Note> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }
}
