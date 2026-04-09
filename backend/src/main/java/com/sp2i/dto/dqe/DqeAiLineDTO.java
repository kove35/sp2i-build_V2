package com.sp2i.dto.dqe;

import java.util.List;

/**
 * Ce DTO represente une ligne DQE enrichie par notre moteur "AI".
 *
 * Il regroupe :
 * - les quantites detectees ou estimees
 * - les prix enrichis
 * - la classification metier
 * - une suggestion fournisseur
 * - un niveau de risque import
 * - une decision achat
 * - un score de confiance
 * - des alertes pour guider l'utilisateur
 *
 * On choisit un record pour garder une structure simple,
 * immuable et tres lisible pour le frontend React.
 */
public record DqeAiLineDTO(
        String designation,
        Double quantite,
        String unite,
        Double prixLocalEstime,
        Double prixImportEstime,
        String lot,
        String famille,
        String fournisseurSuggestion,
        String niveauRisque,
        String decision,
        Double scoreConfiance,
        List<String> alertes
) {
}
