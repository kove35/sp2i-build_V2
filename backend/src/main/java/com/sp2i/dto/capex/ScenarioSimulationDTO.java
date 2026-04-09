package com.sp2i.dto.capex;

/**
 * Ce DTO sert a comparer plusieurs strategies d'achat CAPEX.
 *
 * L'idee est de donner au frontend une lecture simple
 * de trois scenarios :
 * - tout acheter en local
 * - tout acheter en import
 * - prendre le meilleur choix ligne par ligne
 *
 * Cela permet de transformer des donnees techniques
 * en aide a la decision.
 */
public record ScenarioSimulationDTO(
        Double capexLocal,
        Double capexImport,
        Double capexOptimise,
        Double gainImport,
        Double gainOptimise
) {
}
