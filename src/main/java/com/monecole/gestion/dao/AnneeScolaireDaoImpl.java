package com.monecole.gestion.dao;

import com.monecole.gestion.models.AnneeScolaire;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation JDBC du DAO Année Scolaire.
 */
public class AnneeScolaireDaoImpl extends AbstractDao<AnneeScolaire, Long> implements AnneeScolaireDao {

    public AnneeScolaireDaoImpl(DaoFactory daoFactory) {
        super(daoFactory);
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO annees_scolaires (libelle, est_active) VALUES (?, ?)";
    }

    @Override
    protected void setInsertParams(PreparedStatement ps, AnneeScolaire a) throws Exception {
        ps.setString(1, a.libelle());
        ps.setBoolean(2, a.estActive());
    }

    @Override
    protected String getFindByIdSql() {
        return "SELECT id, libelle, est_active FROM annees_scolaires WHERE id = ?";
    }

    @Override
    protected String getFindAllSql() {
        return "SELECT id, libelle, est_active FROM annees_scolaires";
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE annees_scolaires SET libelle = ?, est_active = ? WHERE id = ?";
    }

    @Override
    protected void setUpdateParams(PreparedStatement ps, AnneeScolaire a) throws Exception {
        ps.setString(1, a.libelle());
        ps.setBoolean(2, a.estActive());
        ps.setLong(3, a.id());
    }

    @Override
    protected String getDeleteSql() {
        return "DELETE FROM annees_scolaires WHERE id = ?";
    }

    @Override
    protected String getTableName() {
        return "annees_scolaires";
    }

    @Override
    protected AnneeScolaire copyWithId(AnneeScolaire a, Long id) {
        return new AnneeScolaire(id, a.libelle(), a.estActive());
    }

    @Override
    protected AnneeScolaire mapRow(ResultSet rs) throws Exception {
        return new AnneeScolaire(
            rs.getLong("id"),
            rs.getString("libelle"),
            rs.getBoolean("est_active")
        );
    }

    @Override
    public AnneeScolaire findActive() throws Exception {
        String sql = "SELECT id, libelle, est_active FROM annees_scolaires WHERE est_active = 1";
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? mapRow(rs) : null;
        }
    }

    @Override
    public void setActive(Long id) throws Exception {
        String resetSql = "UPDATE annees_scolaires SET est_active = 0";
        String activateSql = "UPDATE annees_scolaires SET est_active = 1 WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(resetSql)) {
            ps.executeUpdate();
        }
        try (PreparedStatement ps = getConnection().prepareStatement(activateSql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private List<AnneeScolaire> extractList(ResultSet rs) throws Exception {
        List<AnneeScolaire> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }
}
