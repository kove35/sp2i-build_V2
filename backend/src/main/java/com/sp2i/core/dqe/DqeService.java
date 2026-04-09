package com.sp2i.core.dqe;

import com.sp2i.core.exception.BusinessException;
import com.sp2i.domain.capex.CapexItem;
import com.sp2i.domain.capex.PrioriteExecution;
import com.sp2i.domain.project.CapexProject;
import com.sp2i.dto.dqe.CreateDqeItemRequest;
import com.sp2i.dto.dqe.DqeSuggestionDTO;
import com.sp2i.infrastructure.persistence.CapexItemRepository;
import com.sp2i.infrastructure.persistence.CapexProjectRepository;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Ce fichier sert a porter la logique metier du DQE Builder.
 *
 * Il gere trois usages complementaires :
 * - la creation manuelle d'une ligne DQE
 * - les suggestions de familles a partir d'un lot
 * - l'export Excel des lignes deja sauvegardees
 *
 * On reste volontairement proche du vocabulaire metier DQE
 * pour que le code soit facile a suivre pour un debutant.
 */
@Service
public class DqeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DqeService.class);

    /**
     * Suggestions de base.
     *
     * Ce dictionnaire donne une aide simple meme si la base est vide.
     * Ensuite, on complete avec les familles deja observees en base.
     */
    private static final Map<String, List<String>> DEFAULT_FAMILIES_BY_LOT = Map.of(
            "Menuiserie", List.of("Fenetres", "Portes", "Volets"),
            "Electricite", List.of("Tableaux", "Cables", "Eclairage"),
            "Plomberie", List.of("Tuyauterie", "Sanitaires", "Pompage"),
            "CVC", List.of("Climatisation", "Ventilation", "Chauffage"),
            "Finitions", List.of("Peinture", "Revêtement", "Faux plafond")
    );

    private final CapexItemRepository capexItemRepository;
    private final CapexProjectRepository capexProjectRepository;

    public DqeService(
            CapexItemRepository capexItemRepository,
            CapexProjectRepository capexProjectRepository
    ) {
        this.capexItemRepository = capexItemRepository;
        this.capexProjectRepository = capexProjectRepository;
    }

    /**
     * Cree une ligne DQE et la sauvegarde comme CapexItem.
     *
     * Le DQE manipule un prix unitaire.
     * Dans notre modele CAPEX, on le stocke dans coutLocal.
     */
    public CapexItem createDqeItem(CreateDqeItemRequest request) {
        if (request == null) {
            throw new BusinessException("La requete DQE est obligatoire");
        }
        if (request.getProjectId() == null) {
            throw new BusinessException("Le projectId est obligatoire");
        }
        if (request.getQuantite() == null || request.getQuantite() <= 0d) {
            throw new BusinessException("La quantite doit etre > 0");
        }
        if (request.getPrixUnitaire() == null) {
            throw new BusinessException("Le prix unitaire est obligatoire");
        }

        CapexProject project = capexProjectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new BusinessException("Projet introuvable"));

        LOGGER.info(
                "Creation DQE : projet={}, lot={}, famille={}, designation={}",
                request.getProjectId(),
                request.getLot(),
                request.getFamille(),
                request.getDesignation()
        );

        CapexItem item = new CapexItem();
        item.setProject(project);
        item.setLot(trimToNull(request.getLot()));
        item.setFamille(trimToNull(request.getFamille()));
        item.setDesignation(trimToNull(request.getDesignation()));
        item.setUnite(trimToNull(request.getUnite()));
        item.setBatiment(trimToNull(request.getBatiment()));
        item.setNiveau(trimToNull(request.getNiveau()));
        item.setQuantite(request.getQuantite());
        item.setPrixUnitaire(request.getPrixUnitaire());
        item.setCoutImport(null);
        item.setPriorite(PrioriteExecution.MEDIUM);
        item.setDependances(List.of());
        item.setDureeEstimee(1);

        return capexItemRepository.save(item);
    }

    /**
     * Retourne des suggestions de familles pour un lot donne.
     *
     * On combine :
     * - une petite base de connaissances par defaut
     * - les familles deja presentes en base
     */
    public DqeSuggestionDTO getFamilySuggestions(String lot) {
        if (lot == null || lot.isBlank()) {
            return new DqeSuggestionDTO("", List.of());
        }

        TreeSet<String> families = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        families.addAll(DEFAULT_FAMILIES_BY_LOT.getOrDefault(lot, List.of()));

        capexItemRepository.findAll().stream()
                .filter(item -> lot.equalsIgnoreCase(Objects.toString(item.getLot(), "")))
                .map(CapexItem::getFamille)
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .forEach(families::add);

        return new DqeSuggestionDTO(lot, List.copyOf(families));
    }

    /**
     * Exporte toutes les lignes DQE d'un projet dans un fichier Excel.
     */
    public byte[] exportProjectDqe(Long projectId) {
        if (projectId == null) {
            throw new BusinessException("Le projectId est obligatoire");
        }

        CapexProject project = capexProjectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("Projet introuvable"));

        List<CapexItem> items = capexItemRepository.findByProject_Id(projectId);
        LOGGER.info("Export DQE : projet={}, {} ligne(s)", projectId, items.size());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("DQE");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            createCell(headerRow, 0, "lot", headerStyle);
            createCell(headerRow, 1, "famille", headerStyle);
            createCell(headerRow, 2, "designation", headerStyle);
            createCell(headerRow, 3, "unite", headerStyle);
            createCell(headerRow, 4, "quantite", headerStyle);
            createCell(headerRow, 5, "prixUnitaire", headerStyle);
            createCell(headerRow, 6, "prixTotal", headerStyle);

            int rowIndex = 1;
            for (CapexItem item : items) {
                Row row = sheet.createRow(rowIndex++);
                createCell(row, 0, item.getLot(), null);
                createCell(row, 1, item.getFamille(), null);
                createCell(row, 2, item.getDesignation(), null);
                createCell(row, 3, item.getUnite(), null);
                createNumericCell(row, 4, item.getQuantite());
                createNumericCell(row, 5, item.getPrixUnitaire());
                createNumericCell(row, 6, item.getPrixTotal());
            }

            for (int columnIndex = 0; columnIndex <= 6; columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException("Impossible de generer l'export Excel DQE");
        }
    }

    /**
     * Construit un CapexItem a partir d'une ligne DQE deja analysee.
     *
     * Cette methode est aussi reutilisee par le service d'import PDF / image.
     */
    public CapexItem createItemFromParsedLine(Long projectId, ParsedDqeLine parsedLine) {
        CreateDqeItemRequest request = new CreateDqeItemRequest();
        request.setProjectId(projectId);
        request.setLot(parsedLine.lot());
        request.setFamille(parsedLine.famille());
        request.setDesignation(parsedLine.designation());
        request.setUnite(parsedLine.unite());
        request.setBatiment(parsedLine.batiment());
        request.setNiveau(parsedLine.niveau());
        request.setQuantite(parsedLine.quantite());
        request.setPrixUnitaire(parsedLine.prixUnitaire());
        return createDqeItem(request);
    }

    private void createCell(Row row, int cellIndex, String value, CellStyle style) {
        var cell = row.createCell(cellIndex);
        cell.setCellValue(value == null ? "" : value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void createNumericCell(Row row, int cellIndex, Double value) {
        var cell = row.createCell(cellIndex);
        if (value != null) {
            cell.setCellValue(value);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * Representation intermediaire d'une ligne DQE deja comprise
     * par nos services.
     *
     * On utilise un record pour garder un petit objet lisible et immuable.
     */
    public record ParsedDqeLine(
            String lot,
            String famille,
            String designation,
            String unite,
            String batiment,
            String niveau,
            Double quantite,
            Double prixUnitaire,
            Double prixTotal
    ) {
        public Map<String, Object> toDebugMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("lot", lot);
            values.put("famille", famille);
            values.put("designation", designation);
            values.put("unite", unite);
            values.put("batiment", batiment);
            values.put("niveau", niveau);
            values.put("quantite", quantite);
            values.put("prixUnitaire", prixUnitaire);
            values.put("prixTotal", prixTotal);
            return values;
        }
    }
}
