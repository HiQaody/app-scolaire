package com.monecole.gestion.dao;

import com.monecole.gestion.models.Utilisateur;
import java.util.List;

/**
 * Interface DAO pour la gestion des utilisateurs.
 */
public interface UtilisateurDao extends GenericDao<Utilisateur, Long> {
    List<Utilisateur> findByRole(Utilisateur.Role role) throws Exception;
    Utilisateur findByLogin(String login) throws Exception;
}
