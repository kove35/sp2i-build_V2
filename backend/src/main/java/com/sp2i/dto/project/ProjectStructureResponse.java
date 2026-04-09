package com.sp2i.dto.project;

import java.util.List;

/**
 * Ce DTO sert a renvoyer la structure immobiliere d'un projet.
 *
 * On le separe de l'entite JPA pour garder une API claire
 * et pedagogique pour le frontend.
 */
public record ProjectStructureResponse(
        List<BuildingStructureResponse> batiments
) {
    public record BuildingStructureResponse(
            String nom,
            List<String> etages
    ) {
    }
}
