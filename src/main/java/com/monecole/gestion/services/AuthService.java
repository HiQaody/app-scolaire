package com.monecole.gestion.services;

import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.dao.UtilisateurDao;
import com.monecole.gestion.models.Utilisateur;
import com.monecole.gestion.utils.PasswordHasher;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service d'authentification : gère la connexion et la vérification des mots de passe.
 */
public class AuthService {

    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());
    private final UtilisateurDao utilisateurDao;

    public AuthService() {
        this.utilisateurDao = DaoFactory.getInstance().getUtilisateurDao();
    }

    /**
     * Authentifie un utilisateur par login et mot de passe.
     * @return l'utilisateur authentifié, ou null si l'authentification échoue
     */
    public Utilisateur authenticate(String login, String password) {
        try {
            Utilisateur user = utilisateurDao.findByLogin(login);
            if (user == null) {
                return null;
            }
            if (PasswordHasher.verify(password, user.passwordHash())) {
                return user;
            }
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur d'authentification", e);
            return null;
        }
    }

    /**
     * Crée un nouvel utilisateur avec le mot de passe hashé.
     */
    public Utilisateur createUser(String login, String password, Utilisateur.Role role, Long idEnseignant) throws Exception {
        String hash = PasswordHasher.hash(password);
        Utilisateur user = new Utilisateur(null, login, hash, role, idEnseignant);
        return utilisateurDao.create(user);
    }

    /**
     * Change le mot de passe d'un utilisateur existant.
     */
    public void changePassword(Long userId, String newPassword) throws Exception {
        Utilisateur existing = utilisateurDao.findById(userId);
        if (existing == null) {
            throw new IllegalArgumentException("Utilisateur introuvable : " + userId);
        }
        String hash = PasswordHasher.hash(newPassword);
        Utilisateur updated = new Utilisateur(
            existing.id(), existing.login(), hash, existing.role(), existing.idEnseignant()
        );
        utilisateurDao.update(updated);
    }
}
