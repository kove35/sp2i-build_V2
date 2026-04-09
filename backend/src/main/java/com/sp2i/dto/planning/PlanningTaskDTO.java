package com.sp2i.dto.planning;

/**
 * DTO simple d'une tache chantier.
 *
 * Il sert a transformer un CapexItem en une ligne de planning lisible
 * par le frontend ou par Postman.
 *
 * On garde ici seulement les informations demandees :
 * - le lot
 * - la localisation chantier
 * - les dates relatives
 * - la duree
 */
public record PlanningTaskDTO(
        String lot,
        String batiment,
        String niveau,
        String dateDebut,
        String dateFin,
        Integer duree
) {
}
