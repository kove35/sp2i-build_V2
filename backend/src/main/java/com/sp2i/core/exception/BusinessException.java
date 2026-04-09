package com.sp2i.core.exception;

/**
 * Ce fichier sert a representer une erreur metier.
 *
 * Une erreur metier correspond a une regle de gestion non respectee.
 * Exemple :
 * - quantite <= 0
 * - cout local absent
 * - projet introuvable
 *
 * Pourquoi creer une classe dediee ?
 * - pour distinguer clairement les erreurs metier
 *   des erreurs techniques
 * - pour permettre au handler global de renvoyer un code HTTP 400
 * - pour rendre le code plus lisible
 */
public class BusinessException extends RuntimeException {

    /**
     * On transmet simplement le message a la classe parente RuntimeException.
     */
    public BusinessException(String message) {
        super(message);
    }
}
