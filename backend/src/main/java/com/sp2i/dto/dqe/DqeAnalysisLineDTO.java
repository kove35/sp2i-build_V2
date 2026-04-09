package com.sp2i.dto.dqe;

import java.util.List;

/**
 * Ce DTO represente une ligne DQE analysee sans insertion en base.
 *
 * Il sert a expliquer ce que notre moteur a compris :
 * - la designation lue dans le document
 * - les valeurs detectees
 * - la classification proposee
 * - le score de confiance de la ligne
 * - les alertes eventuelles
 */
public record DqeAnalysisLineDTO(
        String designation,
        String lot,
        String famille,
        Double quantite,
        Double prixUnitaire,
        Double prixTotal,
        boolean classee,
        boolean valide,
        double score,
        List<String> alertes
) {
}
