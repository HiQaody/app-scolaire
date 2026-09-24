package com.monecole.gestion.dao;

import com.monecole.gestion.models.Enseignement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation JDBC du DAO Enseignement.
 */
public class EnseignementDaoImpl extends AbstractDao<Enseignement, Long> implements EnseignementDao {

    public EnseignementDaoImpl(DaoFactory daoFactory) {
        super(daoFactory);
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO enseignements (id_enseignant, id_matiere, id_classe) VALUES (?, ?, ?)";
    }

    @Override
    protected void setInsertParams(PreparedStatement ps, Enseignement e) throws Exception {
        ps.setLong(1, e.idEnseignant());
        ps.setLong(2, e.idMatiere());
        ps.setLong(3, e.idClasse());
    }

    @Override
    protected String getFindByIdSql() {
        return "SELECT id, id_enseignant, id_matiere, id_classe FROM enseignements WHERE id = ?";
    }

    @Override
    protected String getFindAllSql() {
        return "SELECT id, id_enseignant, id_matiere, id_classe FROM enseignements";
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE enseignements SET id_enseignant = ?, id_matiere = ?, id_classe = ? WHERE id = ?";
    }

    @Override
    protected void setUpdateParams(PreparedStatement ps, Enseignement e) throws Exception {
        ps.setLong(1, e.idEnseignant());
        ps.setLong(2, e.idMatiere());
        ps.setLong(3, e.idClasse());
        ps.setLong(4, e.id());
    }

    @Override
    protected String getDeleteSql() {
        return "DELETE FROM enseignements WHERE id = ?";
    }

    @Override
    protected String getTableName() {
        return "enseignements";
    }

    @Override
    protected Enseignement copyWithId(Enseignement e, Long id) {
        return new Enseignement(id, e.idEnseignant(), e.idMatiere(), e.idClasse());
    }

    @Override
    protected Enseignement mapRow(ResultSet rs) throws Exception {
        return new Enseignement(
            rs.getLong("id"),
            rs.getLong("id_enseignant"),
            rs.getLong("id_matiere"),
            rs.getLong("id_classe")
        );
    }

    @Override
    public Enseignement findByIds(Long idEnseignant, Long idMatiere, Long idClasse) throws Exception {
        String sql = "SELECT id, id_enseignant, id_matiere, id_classe FROM enseignements WHERE id_enseignant = ? AND id_matiere = ? AND id_classe = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, idEnseignant);
            ps.setLong(2, idMatiere);
            ps.setLong(3, idClasse);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public List<Enseignement> findByIdEnseignant(Long idEnseignant) throws Exception {
        String sql = "SELECT id, id_enseignant, id_matiere, id_classe FROM enseignements WHERE id_enseignant = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, idEnseignant);
            try (ResultSet rs = ps.executeQuery()) {
                return extractList(rs);
            }
        }
    }

    @Override
    public List<Enseignement> findByIdClasse(Long idClasse) throws Exception {
        String sql = "SELECT id, id_enseignant, id_matiere, id_classe FROM enseignements WHERE id_classe = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, idClasse);
            try (ResultSet rs = ps.executeQuery()) {
                return extractList(rs);
            }
        }
    }

    private List<Enseignement> extractList(ResultSet rs) throws Exception {
        List<Enseignement> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }
}
