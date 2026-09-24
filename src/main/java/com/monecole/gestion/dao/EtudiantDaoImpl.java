package com.monecole.gestion.dao;

import com.monecole.gestion.models.Etudiant;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation JDBC du DAO Etudiant.
 */
public class EtudiantDaoImpl extends AbstractDao<Etudiant, Long> implements EtudiantDao {

    public EtudiantDaoImpl(DaoFactory daoFactory) {
        super(daoFactory);
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO etudiants (matricule, nom, prenom, date_naissance, sexe, id_classe) VALUES (?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected void setInsertParams(PreparedStatement ps, Etudiant e) throws Exception {
        ps.setString(1, e.matricule());
        ps.setString(2, e.nom());
        ps.setString(3, e.prenom());
        ps.setObject(4, e.dateNaissance());
        ps.setString(5, e.sexe().name());
        ps.setLong(6, e.idClasse());
    }

    @Override
    protected String getFindByIdSql() {
        return "SELECT id, matricule, nom, prenom, date_naissance, sexe, id_classe FROM etudiants WHERE id = ?";
    }

    @Override
    protected String getFindAllSql() {
        return "SELECT id, matricule, nom, prenom, date_naissance, sexe, id_classe FROM etudiants";
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE etudiants SET matricule = ?, nom = ?, prenom = ?, date_naissance = ?, sexe = ?, id_classe = ? WHERE id = ?";
    }

    @Override
    protected void setUpdateParams(PreparedStatement ps, Etudiant e) throws Exception {
        ps.setString(1, e.matricule());
        ps.setString(2, e.nom());
        ps.setString(3, e.prenom());
        ps.setObject(4, e.dateNaissance());
        ps.setString(5, e.sexe().name());
        ps.setLong(6, e.idClasse());
        ps.setLong(7, e.id());
    }

    @Override
    protected String getDeleteSql() {
        return "DELETE FROM etudiants WHERE id = ?";
    }

    @Override
    protected String getTableName() {
        return "etudiants";
    }

    @Override
    protected Etudiant copyWithId(Etudiant e, Long id) {
        return new Etudiant(id, e.matricule(), e.nom(), e.prenom(), e.dateNaissance(), e.sexe(), e.idClasse());
    }

    @Override
    protected Etudiant mapRow(ResultSet rs) throws Exception {
        return new Etudiant(
            rs.getLong("id"),
            rs.getString("matricule"),
            rs.getString("nom"),
            rs.getString("prenom"),
            rs.getObject("date_naissance") != null ? rs.getObject("date_naissance", LocalDate.class) : null,
            Etudiant.Sexe.valueOf(rs.getString("sexe")),
            rs.getLong("id_classe")
        );
    }

    @Override
    public List<Etudiant> findByIdClasse(Long idClasse) throws Exception {
        String sql = "SELECT id, matricule, nom, prenom, date_naissance, sexe, id_classe FROM etudiants WHERE id_classe = ? ORDER BY nom, prenom";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, idClasse);
            try (ResultSet rs = ps.executeQuery()) {
                return extractList(rs);
            }
        }
    }

    @Override
    public Etudiant findByMatricule(String matricule) throws Exception {
        String sql = "SELECT id, matricule, nom, prenom, date_naissance, sexe, id_classe FROM etudiants WHERE matricule = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, matricule);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    private List<Etudiant> extractList(ResultSet rs) throws Exception {
        List<Etudiant> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }
}
