package com.monecole.gestion.views.grades;

import com.monecole.gestion.dao.ClasseDao;
import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.dao.EnseignementDao;
import com.monecole.gestion.dao.MatiereDao;
import com.monecole.gestion.models.Enseignement;
import com.monecole.gestion.models.Utilisateur;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Résolution des libellés matière et classe des enseignements visibles par
 * l'utilisateur connecté.
 * <p>
 * Les identifiants techniques ne sont jamais affichés : seuls « Matière - Classe »
 * le sont. Un enseignant ne voit que ses propres enseignements, les autres rôles
 * les voient tous.
 */
public final class TeachingLabels {

    private static final Logger LOGGER = Logger.getLogger(TeachingLabels.class.getName());

    private TeachingLabels() {
    }

    /**
     * Enseignement enrichi de ses libellés lisibles.
     *
     * @param id      identifiant de l'enseignement
     * @param matiere libellé de la matière
     * @param classe  libellé de la classe
     */
    public record Teaching(Long id, String matiere, String classe) {

        /** Libellé affiché dans les listes déroulantes. */
        public String libelle() {
            return matiere + " - " + classe;
        }
    }

    /**
     * Enseignements accessibles à l'utilisateur, avec libellés résolus.
     *
     * @return liste ordonnée par matière puis classe
     */
    public static List<Teaching> of(Utilisateur user) throws Exception {
        DaoFactory df = DaoFactory.getInstance();
        List<Enseignement> enseignements = fetch(user, df.getEnseignementDao());

        List<Long> matiereIds = new ArrayList<>();
        List<Long> classeIds = new ArrayList<>();
        for (Enseignement e : enseignements) {
            matiereIds.add(e.idMatiere());
            classeIds.add(e.idClasse());
        }
        Map<Long, String> matieres = libelles(matiereIds, true, df.getMatiereDao(), df.getClasseDao());
        Map<Long, String> classes = libelles(classeIds, false, df.getMatiereDao(), df.getClasseDao());

        List<Teaching> result = new ArrayList<>();
        for (Enseignement e : enseignements) {
            result.add(new Teaching(e.id(),
                matieres.getOrDefault(e.idMatiere(), "?"),
                classes.getOrDefault(e.idClasse(), "?")));
        }
        result.sort((a, b) -> {
            int cmp = a.matiere().compareToIgnoreCase(b.matiere());
            return cmp != 0 ? cmp : a.classe().compareToIgnoreCase(b.classe());
        });
        return result;
    }

    /** Index des enseignements par identifiant, pour résoudre une évaluation. */
    public static Map<Long, Teaching> indexParId(List<Teaching> teachings) {
        Map<Long, Teaching> index = new LinkedHashMap<>();
        for (Teaching t : teachings) {
            index.put(t.id(), t);
        }
        return index;
    }

    private static List<Enseignement> fetch(Utilisateur user, EnseignementDao dao) throws Exception {
        if (user != null
            && user.role() == Utilisateur.Role.ENSEIGNANT
            && user.idEnseignant() != null) {
            return dao.findByIdEnseignant(user.idEnseignant());
        }
        return dao.findAll();
    }

    /** Résout les libellés en un nombre de requêtes proportionnel aux ids distincts. */
    private static Map<Long, String> libelles(List<Long> ids, boolean matiere,
            MatiereDao matiereDao, ClasseDao classeDao) {
        Map<Long, String> result = new LinkedHashMap<>();
        for (Long id : ids) {
            if (id == null || result.containsKey(id)) {
                continue;
            }
            try {
                result.put(id, matiere
                    ? matiereDao.findById(id).libelle()
                    : classeDao.findById(id).nom());
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Libellé introuvable pour l''id " + id, ex);
                result.put(id, "?");
            }
        }
        return result;
    }
}
