package com.sp2i.api;

import com.sp2i.core.dqe.DocumentExtractionService;
import com.sp2i.core.dqe.DqeAiAnalysisService;
import com.sp2i.core.dqe.DqeAnalysisService;
import com.sp2i.core.dqe.DqeFullAnalysisService;
import com.sp2i.core.dqe.DqeImportService;
import com.sp2i.core.dqe.DqeService;
import com.sp2i.domain.capex.CapexItem;
import com.sp2i.infrastructure.persistence.CapexItemRepository;
import com.sp2i.dto.dqe.CreateDqeItemRequest;
import com.sp2i.dto.dqe.DqeAiAnalysisResultDTO;
import com.sp2i.dto.dqe.DqeAnalysisResultDTO;
import com.sp2i.dto.dqe.DqeFullAnalysisResultDTO;
import com.sp2i.dto.dqe.DqeImportResultDTO;
import com.sp2i.dto.dqe.DqeImportValidatedRequestDTO;
import com.sp2i.dto.dqe.DqeSuggestionDTO;
import com.sp2i.dto.dqe.DqeUploadResultDTO;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Ce fichier sert a exposer les fonctions DQE dans une API dediee.
 *
 * On separe ce controller de CapexController pour garder
 * une API lisible :
 * - /capex pour les calculs CAPEX
 * - /dqe pour la construction et l'import des lignes DQE
 */
@RestController
@RequestMapping("/dqe")
public class DqeController {

    private final DocumentExtractionService documentExtractionService;
    private final DqeAiAnalysisService dqeAiAnalysisService;
    private final DqeFullAnalysisService dqeFullAnalysisService;
    private final DqeAnalysisService dqeAnalysisService;
    private final DqeService dqeService;
    private final DqeImportService dqeImportService;
    private final CapexItemRepository capexItemRepository;

    public DqeController(
            DocumentExtractionService documentExtractionService,
            DqeAiAnalysisService dqeAiAnalysisService,
            DqeFullAnalysisService dqeFullAnalysisService,
            DqeAnalysisService dqeAnalysisService,
            DqeService dqeService,
            DqeImportService dqeImportService,
            CapexItemRepository capexItemRepository
    ) {
        this.documentExtractionService = documentExtractionService;
        this.dqeAiAnalysisService = dqeAiAnalysisService;
        this.dqeFullAnalysisService = dqeFullAnalysisService;
        this.dqeAnalysisService = dqeAnalysisService;
        this.dqeService = dqeService;
        this.dqeImportService = dqeImportService;
        this.capexItemRepository = capexItemRepository;
    }

    /**
     * Cree une ligne DQE manuelle puis la sauvegarde en base
     * comme un CapexItem.
     */
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CapexItem createDqeItem(@RequestBody CreateDqeItemRequest request) {
        return dqeService.createDqeItem(request);
    }

    /**
     * Retourne des suggestions de familles a partir d'un lot.
     */
    @GetMapping("/families/suggestions")
    public DqeSuggestionDTO getFamilySuggestions(@RequestParam String lot) {
        return dqeService.getFamilySuggestions(lot);
    }

    /**
     * Upload + extraction de texte standardisee.
     */
    @PostMapping("/upload")
    public DqeUploadResultDTO uploadDqeDocument(@RequestParam("file") MultipartFile file) {
        return documentExtractionService.upload(file);
    }

    /**
     * Importe un document DQE en PDF ou en image.
     */
    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public DqeImportResultDTO importDqeDocument(
            @RequestParam("projectId") Long projectId,
            @RequestParam("file") MultipartFile file
    ) {
        return dqeImportService.importDqeFile(projectId, file);
    }

    /**
     * Importe uniquement les lignes validees depuis la preview frontend.
     */
    @PostMapping(value = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public DqeImportResultDTO importValidatedLines(@RequestBody DqeImportValidatedRequestDTO request) {
        int importedCount = 0;

        if (Boolean.TRUE.equals(request.replaceExisting()) && request.projectId() != null) {
            capexItemRepository.deleteByProject_Id(request.projectId());
        }

        if (request.lignes() != null) {
            for (var line : request.lignes()) {
                CreateDqeItemRequest itemRequest = new CreateDqeItemRequest();
                itemRequest.setProjectId(request.projectId());
                itemRequest.setLot(line.getLot());
                itemRequest.setFamille(line.getFamille());
                itemRequest.setDesignation(line.getDesignation());
                itemRequest.setUnite(line.getUnite());
                itemRequest.setBatiment(line.getBatiment());
                itemRequest.setNiveau(line.getNiveau());
                itemRequest.setQuantite(line.getQuantite());
                itemRequest.setPrixUnitaire(line.getPrixLocalEstime() != null ? line.getPrixLocalEstime() : line.getPrixUnitaire());
                dqeService.createDqeItem(itemRequest);
                importedCount++;
            }
        }

        return new DqeImportResultDTO(importedCount, java.util.List.of());
    }

    /**
     * Analyse un DQE sans l'importer.
     *
     * Ce point d'entree est utile pour une demo ou un pre-controle :
     * on evalue la qualite du document avant insertion en base.
     */
    @PostMapping("/analyze")
    public DqeAnalysisResultDTO analyzeDqeDocument(@RequestParam("file") MultipartFile file) {
        return dqeAnalysisService.analyze(file);
    }

    /**
     * Analyse IA enrichie du DQE.
     *
     * Ici, on va plus loin qu'un simple diagnostic :
     * - estimation des prix manquants
     * - suggestion fournisseur
     * - risque import
     * - decision local / import / mix
     */
    @PostMapping("/analyze-ai")
    public DqeAiAnalysisResultDTO analyzeDqeDocumentWithAi(@RequestParam("file") MultipartFile file) {
        return dqeAiAnalysisService.analyze(file);
    }

    /**
     * Analyse intelligente complete avec KPI CAPEX.
     */
    @PostMapping("/analyze-full")
    public DqeFullAnalysisResultDTO analyzeDqeDocumentFull(@RequestParam("file") MultipartFile file) {
        return dqeFullAnalysisService.analyzeFull(file);
    }

    /**
     * Exporte les lignes d'un projet au format Excel.
     */
    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportProjectDqe(@RequestParam("projectId") Long projectId) {
        byte[] fileContent = dqeService.exportProjectDqe(projectId);
        ByteArrayResource resource = new ByteArrayResource(fileContent);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dqe-builder-export.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .contentLength(fileContent.length)
                .body(resource);
    }
}
