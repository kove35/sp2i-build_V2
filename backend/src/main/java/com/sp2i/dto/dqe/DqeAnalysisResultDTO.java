package com.sp2i.dto.dqe;

import java.util.List;

/**
 * Ce DTO porte le resultat complet d'une analyse DQE.
 *
 * Il donne :
 * - un score global de qualite
 * - les principaux compteurs d'alerte
 * - le detail ligne par ligne pour le frontend
 */
public record DqeAnalysisResultDTO(
        double scoreGlobal,
        int lignesAnalysees,
        int lignesSansPrix,
        int lignesSansQuantite,
        int lignesNonClassees,
        List<DqeAnalysisLineDTO> lignes
) {
}
