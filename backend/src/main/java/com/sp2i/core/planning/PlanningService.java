package com.sp2i.core.planning;

import com.sp2i.core.exception.BusinessException;
import com.sp2i.domain.capex.CapexItem;
import com.sp2i.dto.planning.PlanningTaskDTO;
import com.sp2i.infrastructure.persistence.CapexItemRepository;
import com.sp2i.infrastructure.persistence.CapexProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Ce service transforme les postes CAPEX en taches chantier.
 *
 * Dans cette version volontairement simple et pedagogique :
 * - chaque lot recoit une duree type
 * - chaque lot recoit un ordre metier fixe
 * - les dates sont calculees de facon cumulative
 *
 * Cela permet d'obtenir rapidement un planning chantier lisible
 * sans introduire de moteur complexe.
 */
@Service
public class PlanningService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanningService.class);

    private final CapexItemRepository capexItemRepository;
    private final CapexProjectRepository capexProjectRepository;

    public PlanningService(
            CapexItemRepository capexItemRepository,
            CapexProjectRepository capexProjectRepository
    ) {
        this.capexItemRepository = capexItemRepository;
        this.capexProjectRepository = capexProjectRepository;
    }

    /**
     * Genere la liste ordonnee des taches chantier d'un projet.
     *
     * Etapes :
     * 1. verifier que le projet existe
     * 2. charger ses items CAPEX
     * 3. trier selon l'ordre chantier
     * 4. calculer dateDebut puis dateFin de maniere cumulative
     * 5. retourner une liste de DTO simples
     */
    public List<PlanningTaskDTO> generatePlanning(Long projectId) {
        if (projectId == null) {
            throw new BusinessException("Le projectId est obligatoire pour le planning");
        }

        capexProjectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("Projet introuvable"));

        List<CapexItem> orderedItems = capexItemRepository.findByProject_Id(projectId).stream()
                .sorted(Comparator
                        .comparingInt(this::resolveExecutionOrder)
                        .thenComparing(item -> safeText(item.getBatiment()), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(item -> safeText(item.getNiveau()), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(item -> safeText(item.getLot()), String.CASE_INSENSITIVE_ORDER))
                .toList();

        int currentDay = 0;
        java.util.ArrayList<PlanningTaskDTO> tasks = new java.util.ArrayList<>();
        for (CapexItem item : orderedItems) {
            int duree = resolveEstimatedDuration(item);
            tasks.add(new PlanningTaskDTO(
                    safeText(item.getLot()),
                    safeText(item.getBatiment()),
                    safeText(item.getNiveau()),
                    "jour " + currentDay,
                    "jour " + (currentDay + duree),
                    duree
            ));
            currentDay += duree;
        }

        LOGGER.info("Planning chantier genere pour projectId={} avec {} tache(s)", projectId, tasks.size());
        return tasks;
    }

    /**
     * Duree chantier simplifiee par lot.
     *
     * Regles demandees :
     * - Menuiserie -> 5 jours
     * - Electricite -> 7 jours
     * - Climatisation -> 4 jours
     *
     * Pour les autres lots, on garde une valeur simple de secours : 3 jours.
     */
    private int resolveEstimatedDuration(CapexItem item) {
        String lot = safeText(item.getLot());

        if (lot.equalsIgnoreCase("Menuiserie")) {
            return 5;
        }
        if (lot.equalsIgnoreCase("Electricite")) {
            return 7;
        }
        if (lot.equalsIgnoreCase("Climatisation")) {
            return 4;
        }

        return 3;
    }

    /**
     * Ordre metier fixe du chantier.
     *
     * Regles demandees :
     * - Gros oeuvre = 1
     * - Electricite = 2
     * - Menuiserie = 3
     * - Climatisation = 4
     *
     * Les autres lots vont a la fin.
     */
    private int resolveExecutionOrder(CapexItem item) {
        String lot = safeText(item.getLot());

        if (lot.equalsIgnoreCase("Gros oeuvre")) {
            return 1;
        }
        if (lot.equalsIgnoreCase("Electricite")) {
            return 2;
        }
        if (lot.equalsIgnoreCase("Menuiserie")) {
            return 3;
        }
        if (lot.equalsIgnoreCase("Climatisation")) {
            return 4;
        }

        return 99;
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
