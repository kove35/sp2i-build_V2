package com.sp2i.dto.dqe;

import java.util.List;

/**
 * Ce fichier sert a renvoyer un bilan lisible apres un import DQE.
 *
 * Le frontend peut ainsi afficher :
 * - le nombre de lignes bien importees
 * - les lignes ou motifs qui ont pose probleme
 */
public record DqeImportResultDTO(
        int lignesImportees,
        List<String> erreurs
) {
}
