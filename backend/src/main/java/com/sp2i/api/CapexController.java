package com.sp2i.api;

import com.sp2i.core.capex.CapexService;
import com.sp2i.domain.capex.CapexItem;
import com.sp2i.dto.capex.CreateCapexItemRequest;
import com.sp2i.dto.capex.CapexSummaryDTO;
import com.sp2i.dto.capex.ImportCapexResultDTO;
import com.sp2i.dto.capex.ScenarioSimulationDTO;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Ce fichier sert a exposer le module CAPEX via une API REST.
 *
 * Un controller est la porte d'entree HTTP de l'application.
 * Son role est de :
 * - recevoir une requete HTTP
 * - appeler le service metier approprie
 * - renvoyer une reponse JSON au client
 *
 * Ce controller ne fait pas les calculs lui-meme.
 * Il delegue tout a CapexService.
 * C'est important pour garder une bonne separation des responsabilites.
 */
@RestController
@RequestMapping("/capex")
public class CapexController {

    /**
     * Le service metier est injecte par Spring.
     */
    private final CapexService capexService;

    public CapexController(CapexService capexService) {
        this.capexService = capexService;
    }

    /**
     * Endpoint REST de synthese CAPEX.
     *
     * @GetMapping("/summary") signifie :
     * - on attend une requete HTTP GET
     * - l'URL finale sera /capex/summary
     *
     * Exemple d'appel :
     * GET http://localhost:8080/capex/summary
     *
     * Spring convertit automatiquement le DTO retourne en JSON.
     *
     * Ici, on accepte aussi des filtres optionnels.
     * Cela permet a un dashboard React de demander :
     * - tout le CAPEX
     * - ou seulement une partie du CAPEX
     *
     * Exemple :
     * GET /capex/summary?lot=Menuiserie&famille=Fenetres
     */
    @GetMapping("/summary")
    public CapexSummaryDTO getSummary(
            @RequestParam(required = false) String lot,
            @RequestParam(required = false) String famille,
            @RequestParam(required = false) String batiment,
            @RequestParam(required = false) String niveau,
            @RequestParam(required = false) Long projectId
    ) {
        return capexService.getSummary(lot, famille, batiment, niveau, projectId);
    }

    /**
     * Endpoint REST pour lire tous les postes CAPEX.
     *
     * URL finale :
     * GET /capex/items
     *
     * Cet endpoint est pratique pour verifier les donnees creees
     * depuis Postman ou depuis un futur frontend.
     *
     * Comme pour la synthese, les filtres sont optionnels.
     * On peut donc demander :
     * - tous les items
     * - ou seulement ceux qui correspondent a une selection
     */
    @GetMapping("/items")
    public List<CapexItem> getAllItems(
            @RequestParam(required = false) String lot,
            @RequestParam(required = false) String famille,
            @RequestParam(required = false) String batiment,
            @RequestParam(required = false) String niveau,
            @RequestParam(required = false) Long projectId
    ) {
        return capexService.getAllItems(lot, famille, batiment, niveau, projectId);
    }

    /**
     * Endpoint REST bonus pour lire les postes CAPEX
     * d'un projet precis.
     *
     * URL finale :
     * GET /capex/items/project/{projectId}
     *
     * @PathVariable permet de recuperer la valeur
     * directement depuis l'URL.
     */
    @GetMapping("/items/project/{projectId}")
    public List<CapexItem> getItemsByProjectId(@PathVariable Long projectId) {
        return capexService.getItemsByProjectId(projectId);
    }

    /**
     * Endpoint REST pour comparer plusieurs strategies CAPEX
     * sur un projet donne.
     *
     * URL finale :
     * GET /capex/scenario/{projectId}
     */
    @GetMapping("/scenario/{projectId}")
    public ScenarioSimulationDTO simulateScenarios(@PathVariable Long projectId) {
        return capexService.simulateScenarios(projectId);
    }

    /**
     * Endpoint REST pour lire les derniers items crees.
     *
     * Cette route est pratique apres un import ou une creation manuelle,
     * car elle permet de verifier rapidement les lignes les plus recentes.
     */
    @GetMapping("/items/recent")
    public List<CapexItem> getRecentItems(@RequestParam(required = false) Long projectId) {
        return capexService.getRecentItems(projectId);
    }

    /**
     * Endpoint REST pour creer un nouveau poste CAPEX.
     *
     * URL finale :
     * POST /capex/items
     *
     * Le JSON envoye par le client est lu grace a @RequestBody.
     * Le controller ne valide pas la logique metier lui-meme :
     * il delegue ce travail au service.
     */
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CapexItem createCapexItem(@RequestBody CreateCapexItemRequest request) {
        return capexService.createCapexItem(request);
    }

    /**
     * Endpoint REST pour importer un fichier Excel DQE.
     *
     * URL finale :
     * POST /capex/import
     *
     * On attend :
     * - un fichier Excel dans le champ "file"
     * - un projectId pour rattacher toutes les lignes importees au bon projet
     *
     * Le controller ne lit pas lui-meme le fichier.
     * Il passe simplement le travail au service metier.
     */
    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public ImportCapexResultDTO importCapexFile(
            @RequestParam("projectId") Long projectId,
            @RequestParam("file") MultipartFile file
    ) {
        return capexService.importCapexFile(projectId, file);
    }

    /**
     * Endpoint REST pour telecharger un modele Excel DQE.
     *
     * Ce modele aide l'utilisateur a preparer un fichier
     * compatible avec l'import.
     */
    @GetMapping("/import/template")
    public ResponseEntity<ByteArrayResource> downloadImportTemplate() {
        byte[] fileContent = capexService.generateImportTemplate();
        ByteArrayResource resource = new ByteArrayResource(fileContent);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dqe-import-template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .contentLength(fileContent.length)
                .body(resource);
    }
}
