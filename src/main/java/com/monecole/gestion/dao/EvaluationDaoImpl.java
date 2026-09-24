package com.monecole.gestion.dao;

import com.monecole.gestion.models.Evaluation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation JDBC du DAO Evaluation.
 */
public class EvaluationDaoImpl extends AbstractDao<Evaluation, Long> implements EvaluationDao {

    public EvaluationDaoImpl(DaoFactory daoFactory) {
        super(daoFactory);
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO evaluations (libelle, type, date, id_enseignement) VALUES (?, ?, ?, ?)";
    }

    @Override
    protected void setInsertParams(PreparedStatement ps, Evaluation ev) throws Exception {
        ps.setString(1, ev.libelle());
        ps.setString(2, ev.type().name());
        ps.setObject(3, ev.date());
        ps.setLong(4, ev.idEnseignement());
    }

    @Override
    protected String getFindByIdSql() {
        return "SELECT id, libelle, type, date, id_enseignement FROM evaluations WHERE id = ?";
    }

    @Override
    protected String getFindAllSql() {
        return "SELECT id, libelle, type, date, id_enseignement FROM evaluations";
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE evaluations SET libelle = ?, type = ?, date = ?, id_enseignement = ? WHERE id = ?";
    }

    @Override
    protected void setUpdateParams(PreparedStatement ps, Evaluation ev) throws Exception {
        ps.setString(1, ev.libelle());
        ps.setString(2, ev.type().name());
        ps.setObject(3, ev.date());
        ps.setLong(4, ev.idEnseignement());
        ps.setLong(5, ev.id());
    }

    @Override
    protected String getDeleteSql() {
        return "DELETE FROM evaluations WHERE id = ?";
    }

    @Override
    protected String getTableName() {
        return "evaluations";
    }

    @Override
    protected Evaluation copyWithId(Evaluation ev, Long id) {
        return new Evaluation(id, ev.libelle(), ev.type(), ev.date(), ev.idEnseignement());
    }

    @Override
    protected Evaluation mapRow(ResultSet rs) throws Exception {
        return new Evaluation(
            rs.getLong("id"),
            rs.getString("libelle"),
            Evaluation.TypeEvaluation.fromValue(rs.getString("type")),
            rs.getObject("date") != null ? rs.getObject("date", LocalDate.class) : null,
            rs.getLong("id_enseignement")
        );
    }

    @Override
    public List<Evaluation> findByIdEnseignement(Long idEnseignement) throws Exception {
        String sql = "SELECT id, libelle, type, date, id_enseignement FROM evaluations WHERE id_enseignement = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, idEnseignement);
            try (ResultSet rs = ps.executeQuery()) {
                return extractList(rs);
            }
        }
    }

    @Override
    public List<Evaluation> findByIdClasseAndMatiere(Long idClasse, Long idMatiere) throws Exception {
        String sql = """
            SELECT ev.id, ev.libelle, ev.type, ev.date, ev.id_enseignement
            FROM evaluations ev
            JOIN enseignements e ON ev.id_enseignement = e.id
            WHERE e.id_classe = ? AND e.id_matiere = ?
            """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, idClasse);
            ps.setLong(2, idMatiere);
            try (ResultSet rs = ps.executeQuery()) {
                return extractList(rs);
            }
        }
    }

    private List<Evaluation> extractList(ResultSet rs) throws Exception {
        List<Evaluation> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }
}
