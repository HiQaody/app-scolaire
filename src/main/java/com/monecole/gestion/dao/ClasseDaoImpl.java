package com.monecole.gestion.dao;

import com.monecole.gestion.models.Classe;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation JDBC du DAO Classe.
 */
public class ClasseDaoImpl extends AbstractDao<Classe, Long> implements ClasseDao {

    public ClasseDaoImpl(DaoFactory daoFactory) {
        super(daoFactory);
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO classes (nom, niveau, id_annee) VALUES (?, ?, ?)";
    }

    @Override
    protected void setInsertParams(PreparedStatement ps, Classe c) throws Exception {
        ps.setString(1, c.nom());
        ps.setString(2, c.niveau());
        ps.setLong(3, c.idAnnee());
    }

    @Override
    protected String getFindByIdSql() {
        return "SELECT id, nom, niveau, id_annee FROM classes WHERE id = ?";
    }

    @Override
    protected String getFindAllSql() {
        return "SELECT id, nom, niveau, id_annee FROM classes";
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE classes SET nom = ?, niveau = ?, id_annee = ? WHERE id = ?";
    }

    @Override
    protected void setUpdateParams(PreparedStatement ps, Classe c) throws Exception {
        ps.setString(1, c.nom());
        ps.setString(2, c.niveau());
        ps.setLong(3, c.idAnnee());
        ps.setLong(4, c.id());
    }

    @Override
    protected String getDeleteSql() {
        return "DELETE FROM classes WHERE id = ?";
    }

    @Override
    protected String getTableName() {
        return "classes";
    }

    @Override
    protected Classe copyWithId(Classe c, Long id) {
        return new Classe(id, c.nom(), c.niveau(), c.idAnnee());
    }

    @Override
    protected Classe mapRow(ResultSet rs) throws Exception {
        return new Classe(
            rs.getLong("id"),
            rs.getString("nom"),
            rs.getString("niveau"),
            rs.getLong("id_annee")
        );
    }

    @Override
    public List<Classe> findByIdAnnee(Long idAnnee) throws Exception {
        String sql = "SELECT id, nom, niveau, id_annee FROM classes WHERE id_annee = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, idAnnee);
            try (ResultSet rs = ps.executeQuery()) {
                return extractList(rs);
            }
        }
    }

    private List<Classe> extractList(ResultSet rs) throws Exception {
        List<Classe> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }
}
