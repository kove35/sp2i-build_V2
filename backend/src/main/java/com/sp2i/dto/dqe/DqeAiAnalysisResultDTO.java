package com.sp2i.dto.dqe;

import java.util.List;

/**
 * Ce DTO represente le resultat global d'une analyse IA de DQE.
 *
 * Le frontend a besoin :
 * - d'un score global pour la carte principale
 * - d'une liste d'erreurs ou points d'attention
 * - du detail ligne par ligne
 *
 * On regroupe donc ces informations dans un seul objet
 * pour eviter plusieurs appels API.
 */
public record DqeAiAnalysisResultDTO(
        Double scoreGlobal,
        int lignesAnalysees,
        int lignesAvecAlerte,
        List<String> erreurs,
        List<DqeAiLineDTO> lignes
) {
}
