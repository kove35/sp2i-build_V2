package com.sp2i.api;

import com.sp2i.core.planning.PlanningService;
import com.sp2i.dto.planning.PlanningTaskDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller REST dedie au planning chantier.
 *
 * Il expose un endpoint tres simple :
 * GET /planning/{projectId}
 *
 * La reponse est une liste de taches deja ordonnees.
 */
@RestController
@RequestMapping("/planning")
public class PlanningController {

    private final PlanningService planningService;

    public PlanningController(PlanningService planningService) {
        this.planningService = planningService;
    }

    @GetMapping("/{projectId}")
    public List<PlanningTaskDTO> generatePlanning(@PathVariable Long projectId) {
        return planningService.generatePlanning(projectId);
    }
}
