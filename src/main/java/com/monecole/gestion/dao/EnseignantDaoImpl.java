package com.monecole.gestion.dao;

import com.monecole.gestion.models.Enseignant;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation JDBC du DAO Enseignant.
 */
public class EnseignantDaoImpl extends AbstractDao<Enseignant, Long> implements EnseignantDao {

    public EnseignantDaoImpl(DaoFactory daoFactory) {
        super(daoFactory);
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO enseignants (matricule, nom, prenom) VALUES (?, ?, ?)";
    }

    @Override
    protected void setInsertParams(PreparedStatement ps, Enseignant e) throws Exception {
        ps.setString(1, e.matricule());
        ps.setString(2, e.nom());
        ps.setString(3, e.prenom());
    }

    @Override
    protected String getFindByIdSql() {
        return "SELECT id, matricule, nom, prenom FROM enseignants WHERE id = ?";
    }

    @Override
    protected String getFindAllSql() {
        return "SELECT id, matricule, nom, prenom FROM enseignants";
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE enseignants SET matricule = ?, nom = ?, prenom = ? WHERE id = ?";
    }

    @Override
    protected void setUpdateParams(PreparedStatement ps, Enseignant e) throws Exception {
        ps.setString(1, e.matricule());
        ps.setString(2, e.nom());
        ps.setString(3, e.prenom());
        ps.setLong(4, e.id());
    }

    @Override
    protected String getDeleteSql() {
        return "DELETE FROM enseignants WHERE id = ?";
    }

    @Override
    protected String getTableName() {
        return "enseignants";
    }

    @Override
    protected Enseignant copyWithId(Enseignant e, Long id) {
        return new Enseignant(id, e.matricule(), e.nom(), e.prenom());
    }

    @Override
    protected Enseignant mapRow(ResultSet rs) throws Exception {
        return new Enseignant(
            rs.getLong("id"),
            rs.getString("matricule"),
            rs.getString("nom"),
            rs.getString("prenom")
        );
    }

    @Override
    public Enseignant findByMatricule(String matricule) throws Exception {
        String sql = "SELECT id, matricule, nom, prenom FROM enseignants WHERE matricule = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, matricule);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    private List<Enseignant> extractList(ResultSet rs) throws Exception {
        List<Enseignant> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }
}
