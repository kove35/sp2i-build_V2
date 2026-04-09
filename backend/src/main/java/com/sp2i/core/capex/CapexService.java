package com.sp2i.core.capex;

import com.sp2i.core.exception.BusinessException;
import com.sp2i.domain.capex.CapexItem;
import com.sp2i.domain.capex.PrioriteExecution;
import com.sp2i.domain.project.CapexProject;
import com.sp2i.dto.capex.CreateCapexItemRequest;
import com.sp2i.dto.capex.CapexSummaryDTO;
import com.sp2i.dto.capex.ImportCapexResultDTO;
import com.sp2i.dto.capex.ScenarioSimulationDTO;
import com.sp2i.infrastructure.persistence.CapexItemRepository;
import com.sp2i.infrastructure.persistence.CapexProjectRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ce fichier sert a centraliser toute la logique metier CAPEX.
 *
 * La couche service est l'endroit ou l'on place les regles metier.
 * Ici, cela veut dire :
 * - lire les postes CAPEX depuis le repository
 * - faire les calculs financiers
 * - produire une synthese exploitable par l'API
 *
 * Pourquoi separer le service du controller ?
 * - le controller gere HTTP
 * - le service gere la logique metier
 * - cela rend le code plus facile a tester et a faire evoluer
 */
@Service
public class CapexService {

    /**
     * Logger SLF4J.
     *
     * On l'utilise pour laisser des traces claires dans les logs :
     * combien d'items ont ete calcules, quel est le resultat global, etc.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CapexService.class);

    /**
     * Valeur de secours lorsqu'un lot ou une famille n'est pas renseigne.
     *
     * Cela evite d'avoir des cles null dans les regroupements.
     */
    private static final String UNKNOWN_GROUP = "NON_RENSEIGNE";
    private static final int MIN_EXPECTED_COLUMN_COUNT = 7;

    /**
     * Le repository est injecte par Spring.
     *
     * Le service n'interroge pas directement la base :
     * il passe toujours par la couche repository.
     */
    private final CapexItemRepository capexItemRepository;
    private final CapexProjectRepository capexProjectRepository;
    private final ImportCostCalculator importCostCalculator;

    public CapexService(
            CapexItemRepository capexItemRepository,
            CapexProjectRepository capexProjectRepository,
            ImportCostCalculator importCostCalculator
    ) {
        this.capexItemRepository = capexItemRepository;
        this.capexProjectRepository = capexProjectRepository;
        this.importCostCalculator = importCostCalculator;
    }

    /**
     * Construit la synthese CAPEX complete pour l'API.
     *
     * Etapes :
     * 1. lire tous les items CAPEX depuis la base
     * 2. calculer les montants globaux
     * 3. calculer les regroupements par lot et par famille
     * 4. renvoyer un DTO propre a l'API
     */
    public CapexSummaryDTO getSummary() {
        return getSummary(null, null, null, null, null);
    }

    public CapexSummaryDTO getSummary(String lot, String famille, String batiment, String niveau) {
        return getSummary(lot, famille, batiment, niveau, null);
    }

    /**
     * Construit une synthese CAPEX en tenant compte de filtres optionnels.
     *
     * Pourquoi cette methode est utile ?
     * Un dashboard BI veut souvent recalculer les KPI
     * uniquement sur la selection courante de l'utilisateur.
     *
     * Exemple :
     * - seulement un lot
     * - seulement une famille
     * - ou une combinaison de plusieurs filtres
     */
    public CapexSummaryDTO getSummary(String lot, String famille, String batiment, String niveau, Long projectId) {
        List<CapexItem> items = getFilteredItems(projectId, lot, famille, batiment, niveau);
        LOGGER.info(
                "Calcul CAPEX filtre sur {} item(s) avec projectId={}, lot={}, famille={}, batiment={}, niveau={}",
                items.size(),
                projectId,
                lot,
                famille,
                batiment,
                niveau
        );

        CapexSummaryDTO summary = buildSummary(items, true);

        LOGGER.info(
                "CAPEX calcule: brut={}, optimise={}, economie={}, taux={}",
                summary.capexBrut(),
                summary.capexOptimise(),
                summary.economie(),
                summary.taux()
        );
        return summary;
    }

    /**
     * Retourne tous les postes CAPEX presents en base.
     *
     * Cette methode est utile pour :
     * - afficher les donnees dans Postman
     * - alimenter un futur frontend
     * - verifier facilement ce qui a deja ete cree
     *
     * Ici, la logique metier reste simple :
     * on delegue la lecture au repository et on ajoute un log clair.
     */
    public List<CapexItem> getAllItems() {
        return getAllItems(null, null, null, null, null);
    }

    public List<CapexItem> getAllItems(String lot, String famille, String batiment, String niveau) {
        return getAllItems(lot, famille, batiment, niveau, null);
    }

    /**
     * Retourne les items CAPEX en appliquant les filtres demandés.
     *
     * On garde ici une approche simple et tres lisible :
     * - on lit les items
     * - on applique les filtres metier dans le service
     *
     * Cette approche est pedagogique et pratique pour un projet
     * en phase de construction.
     */
    public List<CapexItem> getAllItems(String lot, String famille, String batiment, String niveau, Long projectId) {
        List<CapexItem> filteredItems = getFilteredItems(projectId, lot, famille, batiment, niveau);
        LOGGER.info(
                "Recuperation des CapexItems filtres : {} element(s) avec projectId={}, lot={}, famille={}, batiment={}, niveau={}",
                filteredItems.size(),
                projectId,
                lot,
                famille,
                batiment,
                niveau
        );
        return filteredItems;
    }

    /**
     * Retourne tous les postes CAPEX d'un projet donne.
     *
     * Cette methode illustre bien le role du service :
     * - verifier que l'entree est correcte
     * - deleguer l'acces aux donnees au repository
     * - garder le controller leger
     */
    public List<CapexItem> getItemsByProjectId(Long projectId) {
        if (projectId == null) {
            LOGGER.warn("Erreur metier : Le projectId est obligatoire.");
            throw new BusinessException("Le projectId est obligatoire.");
        }

        LOGGER.info("Recuperation des CapexItems du projet {}", projectId);
        return capexItemRepository.findByProject_Id(projectId);
    }

    /**
     * Retourne les derniers items crees ou importes.
     *
     * Cette vue est utile pour rassurer l'utilisateur juste apres un import :
     * il peut verifier tout de suite les lignes recemment ajoutees.
     */
    public List<CapexItem> getRecentItems(Long projectId) {
        List<CapexItem> recentItems = projectId == null
                ? capexItemRepository.findTop10ByOrderByIdDesc()
                : capexItemRepository.findTop10ByProject_IdOrderByIdDesc(projectId);
        LOGGER.info("Recuperation des {} derniers CapexItems", recentItems.size());
        return recentItems;
    }

    /**
     * Simule trois scenarios de cout pour un projet :
     * - 100% local
     * - 100% import reel
     * - optimisation ligne par ligne
     *
     * Cette methode aide a comparer rapidement les strategies
     * d'achat ou de sourcing.
     */
    public ScenarioSimulationDTO simulateScenarios(Long projectId) {
        if (projectId == null) {
            throw new BusinessException("Le projectId est obligatoire pour la simulation");
        }

        capexProjectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("Projet introuvable"));

        List<CapexItem> items = capexItemRepository.findByProject_Id(projectId);

        double capexLocal = calculCapexBrut(items);
        double capexImport = calculCapexImport(items);
        double capexOptimise = calculCapexOptimise(items);
        double gainImport = capexLocal - capexImport;
        double gainOptimise = capexLocal - capexOptimise;

        LOGGER.info(
                "Simulation scenario calculee pour projectId={} : local={}, import={}, optimise={}",
                projectId,
                capexLocal,
                capexImport,
                capexOptimise
        );

        return new ScenarioSimulationDTO(
                capexLocal,
                capexImport,
                capexOptimise,
                gainImport,
                gainOptimise
        );
    }

    /**
     * Cree un nouveau poste CAPEX a partir du DTO de requete.
     *
     * Etapes pedagogiques :
     * 1. verifier que les donnees minimales sont presentes
     * 2. verifier que le projet parent existe
     * 3. construire l'entite JPA
     * 4. sauvegarder l'entite
     * 5. renvoyer l'objet cree
     */
    public CapexItem createCapexItem(CreateCapexItemRequest request) {
        if (request == null) {
            LOGGER.warn("Erreur metier : La requete de creation CAPEX est obligatoire.");
            throw new BusinessException("La requete de creation CAPEX est obligatoire.");
        }

        // Validation demandee : la quantite doit etre strictement positive.
        if (request.getQuantite() == null || request.getQuantite() <= 0d) {
            LOGGER.warn("Erreur metier : La quantite doit etre > 0");
            throw new BusinessException("La quantite doit etre > 0");
        }

        // Validation demandee : le cout local doit etre present.
        if (request.getCoutLocal() == null) {
            LOGGER.warn("Erreur metier : Le cout local est obligatoire");
            throw new BusinessException("Le cout local est obligatoire");
        }

        // Validation complementaire utile : on ne cree pas d'item sans projet parent.
        if (request.getProjectId() == null) {
            LOGGER.warn("Erreur metier : Le projectId est obligatoire");
            throw new BusinessException("Le projectId est obligatoire");
        }

        // On charge le projet parent depuis la base pour construire
        // une vraie relation JPA entre CapexItem et CapexProject.
        CapexProject project = capexProjectRepository.findById(request.getProjectId())
                .orElseThrow(() -> {
                    LOGGER.warn("Erreur metier : Projet introuvable");
                    return new BusinessException("Projet introuvable");
                });

        LOGGER.info(
                "Creation CapexItem : lot={}, coutLocal={}",
                request.getLot(),
                request.getCoutLocal()
        );

        // Construction de l'entite a partir du DTO de requete.
        CapexItem capexItem = new CapexItem();
        capexItem.setProject(project);
        capexItem.setLot(request.getLot());
        capexItem.setFamille(request.getFamille());
        capexItem.setBatiment(request.getBatiment());
        capexItem.setNiveau(request.getNiveau());
        capexItem.setQuantite(request.getQuantite());
        capexItem.setCoutLocal(request.getCoutLocal());
        capexItem.setCoutImport(request.getCoutImport());
        capexItem.setDureeEstimee(request.getDureeEstimee());
        capexItem.setOrdreExecution(request.getOrdreExecution());
        capexItem.setPriorite(request.getPriorite() == null ? PrioriteExecution.MEDIUM : request.getPriorite());
        capexItem.setDependances(request.getDependances());

        // save(...) effectue l'insertion en base puis renvoie l'objet persiste.
        CapexItem savedItem = capexItemRepository.save(capexItem);
        LOGGER.info("CapexItem cree avec succes : id={}, projectId={}", savedItem.getId(), savedItem.getProject().getId());
        return savedItem;
    }

    /**
     * Importe un fichier Excel DQE dans le projet indique.
     *
     * Cette methode fait plusieurs choses :
     * 1. verifier que le fichier existe et n'est pas vide
     * 2. verifier que le projet parent existe
     * 3. lire la premiere feuille Excel
     * 4. ignorer la ligne d'entete
     * 5. convertir chaque ligne en CapexItem
     * 6. sauvegarder les items en base
     * 7. renvoyer le nombre de lignes importees
     *
     * Toute la logique reste ici dans le service
     * pour garder le controller simple.
     */
    public ImportCapexResultDTO importCapexFile(Long projectId, MultipartFile file) {
        if (projectId == null) {
            LOGGER.warn("Erreur metier : Le projectId est obligatoire pour l'import");
            throw new BusinessException("Le projectId est obligatoire");
        }

        if (file == null || file.isEmpty()) {
            LOGGER.warn("Erreur metier : Le fichier Excel est vide");
            throw new BusinessException("Le fichier Excel est vide");
        }

        CapexProject project = capexProjectRepository.findById(projectId)
                .orElseThrow(() -> {
                    LOGGER.warn("Erreur metier : Projet introuvable");
                    return new BusinessException("Projet introuvable");
                });

        LOGGER.info("Import DQE demarre pour le projet {} avec le fichier {}", projectId, file.getOriginalFilename());

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            int lignesImportees = 0;

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                // On ignore les lignes completement vides pour rendre l'import
                // plus tolerant aux fichiers bureautiques reels.
                if (row == null || isRowEmpty(row, formatter)) {
                    continue;
                }

                validateRowStructure(row, rowIndex);

                CapexItem capexItem = new CapexItem();
                capexItem.setProject(project);
                capexItem.setLot(readStringCell(row, 0, formatter));
                capexItem.setFamille(readStringCell(row, 1, formatter));
                capexItem.setBatiment(readStringCell(row, 2, formatter));
                capexItem.setNiveau(readStringCell(row, 3, formatter));
                capexItem.setQuantite(readDoubleCell(row, 4, formatter, "quantite"));
                capexItem.setCoutLocal(readDoubleCell(row, 5, formatter, "coutLocal"));
                capexItem.setCoutImport(readDoubleCell(row, 6, formatter, "coutImport"));
                capexItem.setDureeEstimee(1);
                capexItem.setOrdreExecution(null);
                capexItem.setPriorite(PrioriteExecution.MEDIUM);
                capexItem.setDependances(List.of());

                capexItemRepository.save(capexItem);
                lignesImportees++;
            }

            LOGGER.info("Import DQE termine : {} ligne(s) importee(s) pour le projet {}", lignesImportees, projectId);
            return new ImportCapexResultDTO(lignesImportees);
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            LOGGER.error("Erreur technique pendant la lecture du fichier Excel", exception);
            throw new BusinessException("Impossible de lire le fichier Excel");
        } catch (Exception exception) {
            LOGGER.error("Erreur technique pendant l'import du fichier Excel", exception);
            throw new BusinessException("Structure du fichier Excel invalide");
        }
    }

    /**
     * Genere un fichier Excel modele pour l'import DQE.
     *
     * Pourquoi generer ce fichier dans le backend ?
     * - pour garantir que l'utilisateur telecharge toujours
     *   le bon format de colonnes
     * - pour centraliser la definition du modele
     * - pour rendre l'import plus simple a utiliser
     */
    public byte[] generateImportTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("DQE");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            createHeaderCell(headerRow, 0, "lot", headerStyle);
            createHeaderCell(headerRow, 1, "famille", headerStyle);
            createHeaderCell(headerRow, 2, "batiment", headerStyle);
            createHeaderCell(headerRow, 3, "niveau", headerStyle);
            createHeaderCell(headerRow, 4, "quantite", headerStyle);
            createHeaderCell(headerRow, 5, "coutLocal", headerStyle);
            createHeaderCell(headerRow, 6, "coutImport", headerStyle);

            // On ajoute une ligne d'exemple pour que le modele soit
            // auto-explicatif lorsqu'on l'ouvre dans Excel.
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("Menuiserie");
            sampleRow.createCell(1).setCellValue("Fenetres");
            sampleRow.createCell(2).setCellValue("Batiment A");
            sampleRow.createCell(3).setCellValue("R+1");
            sampleRow.createCell(4).setCellValue(10);
            sampleRow.createCell(5).setCellValue(1500);
            sampleRow.createCell(6).setCellValue(1200);

            for (int columnIndex = 0; columnIndex < MIN_EXPECTED_COLUMN_COUNT; columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
            }

            workbook.write(outputStream);
            LOGGER.info("Modele Excel DQE genere avec succes");
            return outputStream.toByteArray();
        } catch (IOException exception) {
            LOGGER.error("Erreur technique pendant la generation du modele Excel", exception);
            throw new BusinessException("Impossible de generer le modele Excel");
        }
    }

    /**
     * Calcule le CAPEX brut.
     *
     * Regle metier :
     * CAPEX brut = somme(coutLocal * quantite)
     *
     * On utilise les streams Java pour exprimer clairement l'idee :
     * - parcourir la liste
     * - transformer chaque item en montant
     * - faire la somme finale
     */
    public double calculCapexBrut(List<CapexItem> items) {
        return items.stream()
                .mapToDouble(item -> defaultValue(item.getCoutLocal()) * defaultValue(item.getQuantite()))
                .sum();
    }

    /**
     * Calcule le CAPEX optimise.
     *
     * Regle metier :
     * pour chaque item, on prend le moins cher entre cout local et cout import reel
     * puis on multiplie par la quantite
     *
     * Formule :
     * min(coutLocal, coutImportReel) * quantite
     */
    public double calculCapexOptimise(List<CapexItem> items) {
        return items.stream()
                .mapToDouble(item -> resolveOptimizedUnitCost(item) * defaultValue(item.getQuantite()))
                .sum();
    }

    /**
     * Calcule le scenario "100% import reel".
     *
     * Ici, on suppose que chaque ligne est achetee en import.
     * Si une ligne n'a pas de prix FOB, on la valorise a 0 en import,
     * ce qui permet d'identifier visuellement les trous de donnees.
     */
    public double calculCapexImport(List<CapexItem> items) {
        return items.stream()
                .mapToDouble(item -> defaultValue(resolveRealImportUnitCost(item)) * defaultValue(item.getQuantite()))
                .sum();
    }

    /**
     * Calcule l'economie realisee grace a l'optimisation.
     *
     * Formule :
     * economie = capexBrut - capexOptimise
     */
    public double calculEconomie(List<CapexItem> items) {
        double capexBrut = calculCapexBrut(items);
        double capexOptimise = calculCapexOptimise(items);
        return capexBrut - capexOptimise;
    }

    /**
     * Calcule le gain immediat total.
     *
     * Ici, on raisonne au niveau du montant total de la ligne :
     * (coutLocal - coutImportReel) * quantite
     *
     * Si le prix Chine est absent, le gain est considere comme nul
     * car on ne peut pas demontrer d'economie.
     */
    public double calculGainTotal(List<CapexItem> items) {
        return items.stream()
                .mapToDouble(this::calculateLineGainAmount)
                .sum();
    }

    /**
     * Compte les lignes qui n'ont pas encore de prix Chine.
     */
    public int countItemsWithoutImportPrice(List<CapexItem> items) {
        return (int) items.stream()
                .filter(item -> item.getCoutImport() == null)
                .count();
    }

    /**
     * Calcule le CAPEX qui n'est pas encore optimisable faute de prix Chine.
     */
    public double calculCapexSansPrixChine(List<CapexItem> items) {
        return items.stream()
                .filter(item -> item.getCoutImport() == null)
                .mapToDouble(item -> defaultValue(item.getCoutLocal()) * defaultValue(item.getQuantite()))
                .sum();
    }

    /**
     * Calcule le taux d'optimisation.
     *
     * Formule :
     * taux = economie / capexBrut
     *
     * Attention :
     * si le CAPEX brut vaut 0, on evite une division par zero
     * et on retourne 0.
     */
    public double tauxOptimisation(List<CapexItem> items) {
        double capexBrut = calculCapexBrut(items);
        if (capexBrut == 0d) {
            return 0d;
        }
        return calculEconomie(items) / capexBrut;
    }

    /**
     * Construit un DTO de synthese a partir d'une liste d'items.
     *
     * Le parametre includeGroups permet de choisir si l'on veut aussi
     * calculer les regroupements par lot et par famille.
     *
     * On reutilise cette methode :
     * - pour le total global
     * - pour les sous-totaux de chaque groupe
     */
    private CapexSummaryDTO buildSummary(List<CapexItem> items, boolean includeGroups) {
        // Calcul des 4 indicateurs principaux.
        double capexBrut = calculCapexBrut(items);
        double capexOptimise = calculCapexOptimise(items);
        double economie = capexBrut - capexOptimise;
        double taux = capexBrut == 0d ? 0d : economie / capexBrut;
        double gainTotal = calculGainTotal(items);
        int nbArticlesSansPrixChine = countItemsWithoutImportPrice(items);
        double capexSansPrixChine = calculCapexSansPrixChine(items);

        // Pour un sous-groupe simple, on n'a pas besoin de recalculer des sous-groupes internes.
        if (!includeGroups) {
            return CapexSummaryDTO.simple(
                    capexBrut,
                    capexOptimise,
                    economie,
                    taux,
                    gainTotal,
                    nbArticlesSansPrixChine,
                    capexSansPrixChine
            );
        }

        // Regroupement des items par lot.
        Map<String, CapexSummaryDTO> parLot = groupSummary(items, CapexItem::getLot);
        Map<String, Double> gainParLot = buildGainMap(parLot);

        // Regroupement des items par famille.
        Map<String, CapexSummaryDTO> parFamille = groupSummary(items, CapexItem::getFamille);
        Map<String, Double> gainParFamille = buildGainMap(parFamille);

        return new CapexSummaryDTO(
                capexBrut,
                capexOptimise,
                economie,
                taux,
                gainTotal,
                nbArticlesSansPrixChine,
                capexSansPrixChine,
                gainParLot,
                gainParFamille,
                parLot,
                parFamille
        );
    }

    /**
     * Regroupe les items suivant une cle donnee, puis calcule une synthese pour chaque groupe.
     *
     * Exemple :
     * - classifier = CapexItem::getLot
     * - classifier = CapexItem::getFamille
     *
     * Pourquoi utiliser Function<CapexItem, String> ?
     * Parce que cela nous permet de reutiliser la meme methode
     * pour plusieurs types de regroupement.
     */
    private Map<String, CapexSummaryDTO> groupSummary(List<CapexItem> items, Function<CapexItem, String> classifier) {
        return items.stream()
                .collect(Collectors.groupingBy(
                        // On normalise la cle pour eviter les nulls ou les chaines vides.
                        item -> normalizeGroupKey(classifier.apply(item)),
                        // Une fois le groupe cree, on calcule sa propre synthese.
                        Collectors.collectingAndThen(Collectors.toList(), groupedItems -> buildSummary(groupedItems, false))
                ));
    }

    /**
     * Transforme une valeur de regroupement potentiellement vide
     * en une valeur lisible et exploitable.
     */
    private String normalizeGroupKey(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN_GROUP;
        }
        return value;
    }

    /**
     * Filtre la liste complete des items en fonction des criteres choisis.
     *
     * Chaque filtre est optionnel :
     * - s'il est vide, on ne filtre pas dessus
     * - s'il est renseigne, on garde uniquement les lignes correspondantes
     *
     * Ce comportement est ideal pour un dashboard dynamique.
     */
    private List<CapexItem> getFilteredItems(Long projectId, String lot, String famille, String batiment, String niveau) {
        return capexItemRepository.findAll().stream()
                .filter(item -> matchesProject(item, projectId))
                .filter(item -> matchesFilter(item.getLot(), lot))
                .filter(item -> matchesFilter(item.getFamille(), famille))
                .filter(item -> matchesFilter(item.getBatiment(), batiment))
                .filter(item -> matchesFilter(item.getNiveau(), niveau))
                .toList();
    }

    /**
     * Compare une valeur venant de la base avec un filtre venant du frontend.
     *
     * On ignore la casse et on considere qu'un filtre vide
     * veut dire "tout accepter".
     */
    private boolean matchesFilter(String itemValue, String selectedFilter) {
        if (selectedFilter == null || selectedFilter.isBlank()) {
            return true;
        }

        if (itemValue == null || itemValue.isBlank()) {
            return false;
        }

        return itemValue.equalsIgnoreCase(selectedFilter);
    }

    private boolean matchesProject(CapexItem item, Long projectId) {
        if (projectId == null) {
            return true;
        }
        return item.getProjectId() != null && item.getProjectId().equals(projectId);
    }

    private Map<String, Double> buildGainMap(Map<String, CapexSummaryDTO> groupedSummary) {
        return groupedSummary.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> defaultValue(entry.getValue().gainTotal())));
    }

    /**
     * Verifie qu'une ligne contient bien le nombre minimal de colonnes attendues.
     *
     * Ici, on veut au minimum les colonnes 0 a 6.
     */
    private void validateRowStructure(Row row, int rowIndex) {
        if (row.getLastCellNum() < MIN_EXPECTED_COLUMN_COUNT) {
            LOGGER.warn("Erreur metier : Structure Excel invalide a la ligne {}", rowIndex + 1);
            throw new BusinessException("Structure du fichier Excel invalide a la ligne " + (rowIndex + 1));
        }
    }

    /**
     * Lit une cellule texte.
     *
     * DataFormatter permet de recuperer une valeur lisible
     * meme si Excel stocke en interne un autre type de cellule.
     */
    private String readStringCell(Row row, int cellIndex, DataFormatter formatter) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return null;
        }

        String value = formatter.formatCellValue(cell);
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Lit une cellule numerique et la convertit en Double.
     *
     * Si la cellule est vide, on retourne null.
     * Si la valeur n'est pas un nombre valide, on renvoie
     * une erreur metier claire avec le nom de la colonne.
     */
    private Double readDoubleCell(Row row, int cellIndex, DataFormatter formatter, String columnName) {
        String rawValue = readStringCell(row, cellIndex, formatter);

        if (rawValue == null) {
            return null;
        }

        String normalizedValue = rawValue.replace(" ", "").replace(",", ".");

        try {
            return Double.parseDouble(normalizedValue);
        } catch (NumberFormatException exception) {
            LOGGER.warn("Erreur metier : Valeur numerique invalide pour {} : {}", columnName, rawValue);
            throw new BusinessException("Valeur numerique invalide pour la colonne " + columnName);
        }
    }

    /**
     * Detecte une ligne totalement vide.
     *
     * C'est utile car beaucoup de fichiers Excel contiennent
     * des lignes vides a la fin ou au milieu.
     */
    private boolean isRowEmpty(Row row, DataFormatter formatter) {
        for (int cellIndex = 0; cellIndex < MIN_EXPECTED_COLUMN_COUNT; cellIndex++) {
            Cell cell = row.getCell(cellIndex);
            if (cell != null && !formatter.formatCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Cree une cellule d'entete avec un style lisible.
     */
    private void createHeaderCell(Row row, int cellIndex, String value, CellStyle headerStyle) {
        Cell cell = row.createCell(cellIndex);
        cell.setCellValue(value);
        cell.setCellStyle(headerStyle);
    }

    /**
     * Gere les valeurs nulles dans les calculs.
     *
     * Pourquoi faire cela ?
     * - pour eviter un NullPointerException
     * - pour garder un calcul robuste meme si la donnees est incomplete
     *
     * Choix metier simple :
     * une valeur absente est consideree comme 0.
     */
    private double defaultValue(Double value) {
        return value == null ? 0d : value;
    }

    private double resolveOptimizedUnitCost(CapexItem item) {
        Double coutImportReel = resolveRealImportUnitCost(item);

        if (coutImportReel == null) {
            return defaultValue(item.getCoutLocal());
        }

        return Math.min(defaultValue(item.getCoutLocal()), coutImportReel);
    }

    private double calculateLineGainAmount(CapexItem item) {
        Double coutImportReel = resolveRealImportUnitCost(item);

        if (coutImportReel == null) {
            return 0d;
        }

        return Math.max(0d, defaultValue(item.getCoutLocal()) - coutImportReel)
                * defaultValue(item.getQuantite());
    }

    /**
     * Transforme le prix FOB stocke dans la base
     * en cout import reel rendu Pointe-Noire.
     *
     * Si aucun prix FOB n'est disponible, on retourne null.
     */
    private Double resolveRealImportUnitCost(CapexItem item) {
        if (item.getCoutImport() == null) {
            return null;
        }

        return importCostCalculator.calculateImportCost(item.getCoutImport());
    }
}
