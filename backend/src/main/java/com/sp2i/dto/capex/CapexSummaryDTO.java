package com.sp2i.dto.capex;

import java.util.Map;

/**
 * Ce fichier sert a transporter une synthese CAPEX vers l'exterieur.
 *
 * DTO signifie Data Transfer Object.
 * Un DTO est un objet fait pour echanger des donnees entre couches,
 * en particulier entre le service metier et l'API REST.
 *
 * Pourquoi ne pas renvoyer directement l'entite JPA ?
 * - parce qu'une entite represente la base de donnees
 * - parce qu'on ne veut pas exposer toute la structure interne
 * - parce qu'un DTO est plus stable et plus lisible pour une API
 *
 * Ici, ce DTO contient :
 * - le CAPEX brut
 * - le CAPEX optimise
 * - l'economie
 * - le taux d'optimisation
 * - des sous-totaux par lot
 * - des sous-totaux par famille
 */
public record CapexSummaryDTO(
        Double capexBrut,
        Double capexOptimise,
        Double economie,
        Double taux,
        Double gainTotal,
        Integer nbArticlesSansPrixChine,
        Double capexSansPrixChine,
        Map<String, Double> gainParLot,
        Map<String, Double> gainParFamille,
        Map<String, CapexSummaryDTO> parLot,
        Map<String, CapexSummaryDTO> parFamille
) {

    /**
     * Methode utilitaire pour creer un DTO "simple"
     * sans les regroupements par lot et par famille.
     *
     * C'est pratique pour reutiliser la meme structure
     * quand on calcule un sous-total pour un groupe.
     */
    public static CapexSummaryDTO simple(
            Double capexBrut,
            Double capexOptimise,
            Double economie,
            Double taux,
            Double gainTotal,
            Integer nbArticlesSansPrixChine,
            Double capexSansPrixChine
    ) {
        return new CapexSummaryDTO(
                capexBrut,
                capexOptimise,
                economie,
                taux,
                gainTotal,
                nbArticlesSansPrixChine,
                capexSansPrixChine,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of()
        );
    }
}
