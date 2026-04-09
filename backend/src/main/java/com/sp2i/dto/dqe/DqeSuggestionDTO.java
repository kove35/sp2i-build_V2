package com.sp2i.dto.dqe;

import java.util.List;

/**
 * Ce DTO transporte des suggestions de familles pour un lot donne.
 *
 * C'est pratique pour rendre le builder DQE plus confortable :
 * l'utilisateur choisit un lot puis voit des familles deja connues.
 */
public record DqeSuggestionDTO(
        String lot,
        List<String> familles
) {
}
