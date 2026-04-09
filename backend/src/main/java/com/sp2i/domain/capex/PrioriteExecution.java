package com.sp2i.domain.capex;

/**
 * Cette enumeration represente la priorite de realisation
 * d'un poste CAPEX dans le planning chantier.
 *
 * On choisit 3 niveaux simples :
 * - LOW
 * - MEDIUM
 * - HIGH
 *
 * Ce format est facile a stocker en base et tres lisible
 * pour un debutant Java.
 */
public enum PrioriteExecution {
    LOW,
    MEDIUM,
    HIGH
}
