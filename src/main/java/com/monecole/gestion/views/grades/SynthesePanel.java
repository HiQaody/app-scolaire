package com.monecole.gestion.views.grades;

import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.models.Etudiant;
import com.monecole.gestion.services.MoyenneService;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * Onglet « Synthèse » : moyenne par matière, moyenne générale pondérée, rang et
 * mention de chaque élève de la classe, calculés sur l'ensemble des évaluations.
 * <p>
 * Le calcul est fourni par l'appelant sous forme d'un {@link Charge}, obtenu en
 * arrière-plan, puis appliqué ici sur l'EDT.
 */
public class SynthesePanel extends JPanel {

    private final SyntheseModel model = new SyntheseModel();
    private final JTable table = new JTable(model);
    private final JLabel resumeLabel = new JLabel(" ");

    public SynthesePanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(26);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setDefaultRenderer(Object.class, new SyntheseRenderer());
        table.getTableHeader().setReorderingAllowed(false);

        JPanel entete = new JPanel(new BorderLayout());
        entete.setOpaque(false);
        JLabel titre = new JLabel("Synthèse de la classe (moyennes ramenées sur 20)");
        titre.setFont(titre.getFont().deriveFont(Font.BOLD, 13));
        entete.add(titre, BorderLayout.WEST);
        entete.add(resumeLabel, BorderLayout.EAST);

        add(entete, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        appliquer(Charge.VIDE);
    }

    /** Vide le panneau. */
    public void clear() {
        appliquer(Charge.VIDE);
    }

    /** Applique un résultat de synthèse calculé hors EDT. */
    public void appliquer(Charge charge) {
        model.setDonnees(charge.synthese(), charge.matieresIds(), charge.matieresLibelles());
        resumeLabel.setText(resume(model.effectifNotes(), model.moyenneClasse()));
    }

    /**
     * Prépare la matière d'affichage d'une synthèse : ordonne les identifiants de
     * matière puis résout leurs libellés. À appeler en arrière-plan.
     */
    public static Charge preparer(List<MoyenneService.SyntheseEtudiant> synthese) throws Exception {
        List<Long> ids = new ArrayList<>();
        for (MoyenneService.SyntheseEtudiant s : synthese) {
            for (Long idMatiere : s.moyennesParMatiere().keySet()) {
                if (!ids.contains(idMatiere)) {
                    ids.add(idMatiere);
                }
            }
        }
        ids.sort(Long::compare);
        return new Charge(synthese, ids, libellesMatieres(ids));
    }

    private static List<String> libellesMatieres(List<Long> matieres) throws Exception {
        var matiereDao = DaoFactory.getInstance().getMatiereDao();
        List<String> libelles = new ArrayList<>(matieres.size());
        for (Long id : matieres) {
            var matiere = matiereDao.findById(id);
            libelles.add(matiere == null ? "Matière " + id : matiere.libelle());
        }
        return libelles;
    }

    private static String resume(int effectif, Double moyenneClasse) {
        if (effectif == 0) {
            return "Aucun élève noté";
        }
        return effectif + " élève(s) noté(s) • moyenne de la classe : "
            + String.format("%.2f", moyenneClasse) + "/20";
    }

    /** Résultat de synthèse transporté de l'arrière-plan vers l'EDT. */
    public record Charge(
        List<MoyenneService.SyntheseEtudiant> synthese,
        List<Long> matieresIds,
        List<String> matieresLibelles
    ) {
        public static final Charge VIDE = new Charge(List.of(), List.of(), List.of());

        public Charge {
            synthese = List.copyOf(synthese);
            matieresIds = List.copyOf(matieresIds);
            matieresLibelles = List.copyOf(matieresLibelles);
        }
    }

    /** Renderer : alterne les lignes et met en valeur la moyenne générale. */
    private static class SyntheseRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component comp = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                comp.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF5, 0xF6, 0xFA));
            }
            setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
            boolean colonneMoyenne = column == table.getColumnCount() - 2;
            if (colonneMoyenne) {
                setForeground("—".equals(value) ? Color.GRAY : new Color(0x1E, 0x88, 0x3E));
                setFont(getFont().deriveFont(Font.BOLD));
            } else if (!isSelected) {
                setForeground(table.getForeground());
                setFont(table.getFont());
            }
            return comp;
        }
    }

    /** Modèle à colonnes dynamiques : une colonne par matière puis la synthèse. */
    private static class SyntheseModel extends AbstractTableModel {
        private static final String[] COLONNES_FIXES = {
            "Rang", "Nom", "Prénom", "Moyenne générale", "Mention"
        };

        private List<MoyenneService.SyntheseEtudiant> donnees = new ArrayList<>();
        private List<Long> matieresIds = new ArrayList<>();
        private List<String> matieresLibelles = new ArrayList<>();

        void setDonnees(List<MoyenneService.SyntheseEtudiant> donnees,
                List<Long> matieresIds, List<String> matieresLibelles) {
            this.donnees = new ArrayList<>(donnees);
            this.matieresIds = new ArrayList<>(matieresIds);
            this.matieresLibelles = new ArrayList<>(matieresLibelles);
            fireTableStructureChanged();
        }

        int effectifNotes() {
            int n = 0;
            for (MoyenneService.SyntheseEtudiant s : donnees) {
                if (!s.sansNote()) {
                    n++;
                }
            }
            return n;
        }

        Double moyenneClasse() {
            double somme = 0;
            int n = 0;
            for (MoyenneService.SyntheseEtudiant s : donnees) {
                if (!s.sansNote()) {
                    somme += s.moyenneGenerale();
                    n++;
                }
            }
            return n == 0 ? null : somme / n;
        }

        @Override
        public int getRowCount() {
            return donnees.size();
        }

        @Override
        public int getColumnCount() {
            return COLONNES_FIXES.length + matieresLibelles.size();
        }

        @Override
        public String getColumnName(int column) {
            if (column < matieresLibelles.size()) {
                return matieresLibelles.get(column);
            }
            int index = column - matieresLibelles.size();
            return index < COLONNES_FIXES.length ? COLONNES_FIXES[index] : "";
        }

        @Override
        public Object getValueAt(int row, int column) {
            MoyenneService.SyntheseEtudiant s = donnees.get(row);
            Etudiant e = s.etudiant();
            if (column < matieresLibelles.size()) {
                Double moyenne = s.moyennesParMatiere().get(matieresIds.get(column));
                return moyenne == null ? "" : String.format("%.2f", moyenne);
            }
            return switch (column - matieresLibelles.size()) {
                case 0 -> s.sansNote() ? "" : s.rang() + (s.rang() == 1 ? "er" : "e");
                case 1 -> e.nom();
                case 2 -> e.prenom();
                case 3 -> s.sansNote() ? "—" : String.format("%.2f", s.moyenneGenerale());
                case 4 -> s.mention() == null ? "" : s.mention().getLabel();
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}
