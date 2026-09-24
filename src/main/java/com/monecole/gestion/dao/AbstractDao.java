package com.monecole.gestion.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation abstraite de GenericDao fourissant les opérations CRUD communes.
 * Les sous-classes concrètes définissent les requêtes SQL via les méthodes abstraites.
 *
 * @param <T>  le type d'entité
 * @param <ID> le type de l'identifiant
 */
public abstract class AbstractDao<T, ID> implements GenericDao<T, ID> {

    protected final DaoFactory daoFactory;

    protected AbstractDao(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    protected Connection getConnection() throws Exception {
        return daoFactory.getConnection();
    }

    /** Retourne la requête INSERT avec paramètres positionnels. */
    protected abstract String getInsertSql();

    /** Remplit les paramètres de l'INSERT (index 1..n). */
    protected abstract void setInsertParams(PreparedStatement ps, T entity) throws Exception;

    /** Retourne la requête SELECT BY ID. */
    protected abstract String getFindByIdSql();

    /** Retourne la requête SELECT ALL. */
    protected abstract String getFindAllSql();

    /** Convertit une ligne de ResultSet en entité. */
    protected abstract T mapRow(ResultSet rs) throws Exception;

    /** Retourne la requête UPDATE. */
    protected abstract String getUpdateSql();

    /** Remplit les paramètres de l'UPDATE (index 1..n). */
    protected abstract void setUpdateParams(PreparedStatement ps, T entity) throws Exception;

    /** Retourne la requête DELETE BY ID. */
    protected abstract String getDeleteSql();

    /** Retourne la requête COUNT. */
    protected String getCountSql() {
        return "SELECT COUNT(*) FROM " + getTableName();
    }

    /** Retourne le nom de la table pour les requêtes générées. */
    protected abstract String getTableName();

    /** Retourne une copie de l'entité avec l'ID généré assigné. */
    protected abstract T copyWithId(T entity, Long id);

    @Override
    public T create(T entity) throws Exception {
        try (PreparedStatement ps = getConnection().prepareStatement(getInsertSql(), Statement.RETURN_GENERATED_KEYS)) {
            setInsertParams(ps, entity);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return copyWithId(entity, rs.getLong(1));
                }
            }
            return entity;
        }
    }

    @Override
    public T findById(ID id) throws Exception {
        try (PreparedStatement ps = getConnection().prepareStatement(getFindByIdSql())) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public List<T> findAll() throws Exception {
        try (PreparedStatement ps = getConnection().prepareStatement(getFindAllSql());
             ResultSet rs = ps.executeQuery()) {

            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRow(rs));
            }
            return results;
        }
    }

    @Override
    public void update(T entity) throws Exception {
        try (PreparedStatement ps = getConnection().prepareStatement(getUpdateSql())) {
            setUpdateParams(ps, entity);
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(ID id) throws Exception {
        try (PreparedStatement ps = getConnection().prepareStatement(getDeleteSql())) {
            ps.setObject(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public long count() throws Exception {
        try (PreparedStatement ps = getConnection().prepareStatement(getCountSql());
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

}