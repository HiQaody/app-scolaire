package com.monecole.gestion.models;

import java.time.LocalDate;

/**
 * Projection d'une note enrichie du contexte d'évaluation, de matière et de classe.
 * <p>
 * Permet de calculer moyennes, coefficients et statistiques à partir d'une
 * seule requête au lieu d'une requête par note (problème N+1).
 *
 * @param idNote           identifiant de la note
 * @param valeur           note brute, ramenée ou non sur le barème de l'évaluation
 * @param idEtudiant       étudiant concerné
 * @param idEvaluation     évaluation concernée
 * @param idEnseignement    enseignement porteur de l'évaluation
 * @param idMatiere        matière de l'enseignement
 * @param idClasse         classe de l'étudiant
 * @param codeMatiere      code de la matière
 * @param libelleMatiere   libellé de la matière
 * @param coefficient      coefficient de la matière
 * @param libelleEvaluation libellé de l'évaluation
 * @param typeEvaluation   type de l'évaluation
 * @param dateEvaluation   date de l'évaluation
 * @param poids            pondération de l'évaluation dans la moyenne de la matière
 * @param bareme           note maximale de l'évaluation
 */
public record NoteDetail(
    Long idNote,
    Double valeur,
    Long idEtudiant,
    Long idEvaluation,
    Long idEnseignement,
    Long idMatiere,
    Long idClasse,
    String codeMatiere,
    String libelleMatiere,
    double coefficient,
    String libelleEvaluation,
    Evaluation.TypeEvaluation typeEvaluation,
    LocalDate dateEvaluation,
    double poids,
    double bareme
) {
    /** Note ramenée sur 20 : une note de 15 sur 20 vaut 15, une note de 8 sur 10 vaut 16. */
    public double valeurSur20() {
        if (valeur == null || bareme <= 0) {
            return 0;
        }
        return valeur * 20.0 / bareme;
    }

    /** Contribution pondérée de la note (valeur sur 20 × poids de l'évaluation). */
    public double contribution() {
        return valeurSur20() * poids;
    }
}
