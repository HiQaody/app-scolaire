package com.monecole.gestion.views.grades;

/**
 * Élément d'une liste déroulante portant son identifiant métier.
 * <p>
 * Évite d'afficher un identifiant technique dans le libellé et de le
 * retrouver par analyse de chaîne : le libellé reste purely lisible et
 * l'identifiant voyage avec l'objet.
 *
 * @param id    identifiant en base
 * @param label libellé affiché
 */
public record ComboItem(Long id, String label) {

    @Override
    public String toString() {
        return label;
    }

    /** Libellé d'une évaluation : matière, classe, type, intitulé et date. */
    public static ComboItem pourEvaluation(Long id, String matiere, String classe,
            String type, String libelle, String date) {
        return new ComboItem(id,
            matiere + " · " + classe + " · " + type + " " + libelle + " (" + date + ")");
    }
}
