package com.monecole.gestion.services;

import com.monecole.gestion.dao.AnneeScolaireDao;
import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.models.AnneeScolaire;
import com.monecole.gestion.models.Classe;
import com.monecole.gestion.models.Enseignement;
import com.monecole.gestion.models.Etudiant;
import com.monecole.gestion.models.Matiere;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service de génération des bulletins de notes en PDF via JasperReports (Sprint 5).
 * Compile le template .jrxml, le remplit avec les moyennes calculées par {@link MoyenneService}
 * et exporte le résultat au format PDF.
 */
public class BulletinService {

    private static final Logger LOGGER = Logger.getLogger(BulletinService.class.getName());
    private static final String TEMPLATE_PATH = "/reports/bulletin.jrxml";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MoyenneService moyenneService;
    private final AnneeScolaireDao anneeScolaireDao;

    public BulletinService() {
        this.moyenneService = new MoyenneService();
        this.anneeScolaireDao = DaoFactory.getInstance().getAnneeScolaireDao();
    }

    /**
     * Une ligne du tableau de notes du bulletin (une par matière).
     * Clés de la map : matiere, coefficient, moyenneMatiere, mentionMatiere.
     */
    public record LigneBulletin(String matiere, Double coefficient, String moyenneMatiere, String mentionMatiere) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("matiere", matiere);
            map.put("coefficient", coefficient);
            map.put("moyenneMatiere", moyenneMatiere);
            map.put("mentionMatiere", mentionMatiere);
            return map;
        }
    }

    /**
     * Génère le bulletin PDF d'un étudiant pour un trimestre donné.
     *
     * @param etudiant  l'étudiant concerné
     * @param trimestre libellé du trimestre (ex: "Trimestre 1"), ou "Année complète"
     * @return le chemin du fichier PDF généré
     * @throws Exception si la compilation, le remplissage ou l'export échoue
     */
    public Path genererBulletin(Etudiant etudiant, String trimestre) throws Exception {
        Classe classe = null;
        if (etudiant.idClasse() != null) {
            classe = DaoFactory.getInstance().getClasseDao().findById(etudiant.idClasse());
        }

        List<LigneBulletin> lignes = construireLignes(etudiant, classe);
        Map<String, Object> parametres = construireParametres(etudiant, classe, trimestre, lignes);

        return exporterPdf(etudiant, trimestre, lignes, parametres);
    }

    private List<LigneBulletin> construireLignes(Etudiant etudiant, Classe classe) throws Exception {
        List<LigneBulletin> lignes = new ArrayList<>();
        if (classe == null) {
            return lignes;
        }

        List<Enseignement> enseignements =
            DaoFactory.getInstance().getEnseignementDao().findByIdClasse(classe.id());
        for (Enseignement ens : enseignements) {
            Matiere matiere = DaoFactory.getInstance().getMatiereDao().findById(ens.idMatiere());
            if (matiere == null) {
                continue;
            }
            OptionalDouble moy = moyenneService.moyenneMatiere(etudiant.id(), matiere.id(), classe.id());
            lignes.add(new LigneBulletin(
                matiere.libelle() + " (" + matiere.code() + ")",
                matiere.coefficient(),
                moy.isPresent() ? String.format("%.2f", moy.getAsDouble()) : "—",
                moy.isPresent() ? MoyenneService.getMention(moy.getAsDouble()).getLabel() : "—"
            ));
        }
        lignes.sort((a, b) -> a.matiere().compareToIgnoreCase(b.matiere()));
        return lignes;
    }

    private Map<String, Object> construireParametres(
            Etudiant etudiant, Classe classe, String trimestre, List<LigneBulletin> lignes) throws Exception {

        Map<String, Object> params = new HashMap<>();
        params.put("etablissement", "Établissement Scolaire");
        params.put("anneeScolaire", libelleAnneeActive());
        params.put("trimestre", trimestre);

        params.put("matricule", etudiant.matricule());
        params.put("nom", etudiant.nom());
        params.put("prenom", etudiant.prenom());
        params.put("classe", classe != null ? classe.nom() : "—");
        params.put("niveau", classe != null ? classe.niveau() : "—");

        if (classe != null) {
            OptionalDouble moyGen = moyenneService.moyenneGenerale(etudiant.id(), classe.id());
            params.put("moyenneGenerale", moyGen.isPresent() ? String.format("%.2f", moyGen.getAsDouble()) : "—");

            List<MoyenneService.EtudiantMoyenne> classement = moyenneService.classementClasse(classe.id());
            int rang = 0;
            for (int i = 0; i < classement.size(); i++) {
                if (classement.get(i).etudiant().id().equals(etudiant.id())) {
                    rang = i + 1;
                    break;
                }
            }
            params.put("rang", rang > 0 ? String.valueOf(rang) : "—");
            params.put("effectif", String.valueOf(classement.size()));

            OptionalDouble moyClasse = moyenneService.moyenneClasse(classe.id());
            params.put("moyenneClasse", moyClasse.isPresent() ? String.format("%.2f", moyClasse.getAsDouble()) : "—");

            params.put("mention", moyGen.isPresent()
                ? MoyenneService.getMention(moyGen.getAsDouble()).getLabel()
                : "—");
        } else {
            params.put("moyenneGenerale", "—");
            params.put("rang", "—");
            params.put("effectif", "—");
            params.put("moyenneClasse", "—");
            params.put("mention", "—");
        }
        return params;
    }

    private String libelleAnneeActive() {
        try {
            AnneeScolaire annee = anneeScolaireDao.findActive();
            return annee != null ? annee.libelle() : "—";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur récupération année scolaire active", e);
            return "—";
        }
    }

    private Path exporterPdf(Etudiant etudiant, String trimestre, List<LigneBulletin> lignes,
                             Map<String, Object> parametres) throws Exception {
        // 1. Compiler le template .jrxml à la volée (pas besoin de .jasper pré-compilé)
        JasperReport report;
        try (InputStream templateStream = getClass().getResourceAsStream(TEMPLATE_PATH)) {
            if (templateStream == null) {
                throw new IllegalStateException("Template introuvable : " + TEMPLATE_PATH);
            }
            report = JasperCompileManager.compileReport(templateStream);
        }

        // 2. Remplir avec la liste des lignes de notes (JRMapCollectionDataSource : compatible
        // avec les records Java car JRBeanCollectionDataSource/beanutils ne les supporte pas)
        List<Map<String, Object>> rows = lignes.stream().map(LigneBulletin::toMap).toList();
        JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(new ArrayList<Map<String, ?>>(rows));
        JasperPrint print = JasperFillManager.fillReport(report, parametres, dataSource);

        // 3. Exporter en PDF dans le dossier Documents, ou à défaut le home
        Path outputDir = resolverDossierSortie();
        String nomFichier = String.format("Bulletin_%s_%s.pdf",
            sanitize(etudiant.matricule() != null ? etudiant.matricule() : String.valueOf(etudiant.id())),
            sanitize(trimestre));
        Path pdfPath = outputDir.resolve(nomFichier);

        JasperExportManager.exportReportToPdfFile(print, pdfPath.toString());
        LOGGER.log(Level.INFO, "Bulletin généré : {0}", pdfPath);
        return pdfPath;
    }

    private Path resolverDossierSortie() throws Exception {
        Path documents = Paths.get(System.getProperty("user.home"), "Documents");
        Path target = Files.isDirectory(documents) ? documents : Paths.get(System.getProperty("user.home"));
        Files.createDirectories(target);
        return target;
    }

    private String sanitize(String value) {
        return value == null ? "inconnu" : value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
