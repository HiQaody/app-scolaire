package com.monecole.gestion.dao;

import com.monecole.gestion.models.Matiere;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation JDBC du DAO Matiere.
 */
public class MatiereDaoImpl extends AbstractDao<Matiere, Long> implements MatiereDao {

    public MatiereDaoImpl(DaoFactory daoFactory) {
        super(daoFactory);
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO matieres (code, libelle, coefficient) VALUES (?, ?, ?)";
    }

    @Override
    protected void setInsertParams(PreparedStatement ps, Matiere m) throws Exception {
        ps.setString(1, m.code());
        ps.setString(2, m.libelle());
        ps.setDouble(3, m.coefficient());
    }

    @Override
    protected String getFindByIdSql() {
        return "SELECT id, code, libelle, coefficient FROM matieres WHERE id = ?";
    }

    @Override
    protected String getFindAllSql() {
        return "SELECT id, code, libelle, coefficient FROM matieres";
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE matieres SET code = ?, libelle = ?, coefficient = ? WHERE id = ?";
    }

    @Override
    protected void setUpdateParams(PreparedStatement ps, Matiere m) throws Exception {
        ps.setString(1, m.code());
        ps.setString(2, m.libelle());
        ps.setDouble(3, m.coefficient());
        ps.setLong(4, m.id());
    }

    @Override
    protected String getDeleteSql() {
        return "DELETE FROM matieres WHERE id = ?";
    }

    @Override
    protected String getTableName() {
        return "matieres";
    }

    @Override
    protected Matiere copyWithId(Matiere m, Long id) {
        return new Matiere(id, m.code(), m.libelle(), m.coefficient());
    }

    @Override
    protected Matiere mapRow(ResultSet rs) throws Exception {
        return new Matiere(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("libelle"),
            rs.getDouble("coefficient")
        );
    }

    @Override
    public Matiere findByCode(String code) throws Exception {
        String sql = "SELECT id, code, libelle, coefficient FROM matieres WHERE code = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    private List<Matiere> extractList(ResultSet rs) throws Exception {
        List<Matiere> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }
}
