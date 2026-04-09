package com.sp2i.dto.dqe;

/**
 * Ce DTO represente l'audit d'un bloc de DQE.
 *
 * Un bloc correspond ici a une zone metier lisible du document,
 * par exemple :
 * - un batiment
 * - un niveau ou une section
 * - un sous-total de fin de bloc
 *
 * Le but est de comparer :
 * - le sous-total lu dans le PDF
 * - la somme recalculee a partir des lignes detectees
 */
public record DqeAuditBlockDTO(
        String batiment,
        String niveau,
        Double sousTotalDocument,
        Double sousTotalCalcule,
        Double ecart,
        boolean coherent,
        int lignesDetectees
) {
}
