package com.monecole.gestion.dao;

import com.monecole.gestion.models.Utilisateur;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation JDBC du DAO Utilisateur.
 */
public class UtilisateurDaoImpl extends AbstractDao<Utilisateur, Long> implements UtilisateurDao {

    public UtilisateurDaoImpl(DaoFactory daoFactory) {
        super(daoFactory);
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO utilisateurs (login, password_hash, role, id_enseignant) VALUES (?, ?, ?, ?)";
    }

    @Override
    protected void setInsertParams(PreparedStatement ps, Utilisateur u) throws Exception {
        ps.setString(1, u.login());
        ps.setString(2, u.passwordHash());
        ps.setString(3, u.role().name());
        ps.setObject(4, u.idEnseignant());
    }

    @Override
    protected String getFindByIdSql() {
        return "SELECT id, login, password_hash, role, id_enseignant FROM utilisateurs WHERE id = ?";
    }

    @Override
    protected String getFindAllSql() {
        return "SELECT id, login, password_hash, role, id_enseignant FROM utilisateurs";
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE utilisateurs SET login = ?, password_hash = ?, role = ?, id_enseignant = ? WHERE id = ?";
    }

    @Override
    protected void setUpdateParams(PreparedStatement ps, Utilisateur u) throws Exception {
        ps.setString(1, u.login());
        ps.setString(2, u.passwordHash());
        ps.setString(3, u.role().name());
        ps.setObject(4, u.idEnseignant());
        ps.setLong(5, u.id());
    }

    @Override
    protected String getDeleteSql() {
        return "DELETE FROM utilisateurs WHERE id = ?";
    }

    @Override
    protected String getTableName() {
        return "utilisateurs";
    }

    @Override
    protected Utilisateur copyWithId(Utilisateur u, Long id) {
        return new Utilisateur(id, u.login(), u.passwordHash(), u.role(), u.idEnseignant());
    }

    @Override
    protected Utilisateur mapRow(ResultSet rs) throws Exception {
        return new Utilisateur(
            rs.getLong("id"),
            rs.getString("login"),
            rs.getString("password_hash"),
            Utilisateur.Role.fromValue(rs.getString("role")),
            rs.getObject("id_enseignant") != null ? rs.getLong("id_enseignant") : null
        );
    }

    @Override
    public List<Utilisateur> findByRole(Utilisateur.Role role) throws Exception {
        String sql = "SELECT id, login, password_hash, role, id_enseignant FROM utilisateurs WHERE role = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                return extractList(rs);
            }
        }
    }

    @Override
    public Utilisateur findByLogin(String login) throws Exception {
        String sql = "SELECT id, login, password_hash, role, id_enseignant FROM utilisateurs WHERE login = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    private List<Utilisateur> extractList(ResultSet rs) throws Exception {
        List<Utilisateur> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }
}
