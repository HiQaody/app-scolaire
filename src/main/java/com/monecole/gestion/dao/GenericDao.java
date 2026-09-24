package com.monecole.gestion.dao;

import java.util.List;

/**
 * Interface générique de DAO (Data Access Object) pour les opérations CRUD.
 * @param <T>  le type d'entité
 * @param <ID> le type de l'identifiant
 */
public interface GenericDao<T, ID> {

    T create(T entity) throws Exception;

    T findById(ID id) throws Exception;

    List<T> findAll() throws Exception;

    void update(T entity) throws Exception;

    void delete(ID id) throws Exception;

    long count() throws Exception;
}
