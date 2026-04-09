package com.sp2i.dto.error;

import java.time.LocalDateTime;

/**
 * Ce fichier sert a decrire le format JSON de nos erreurs API.
 *
 * Quand une erreur se produit, on prefere renvoyer un JSON clair
 * plutot qu'une erreur technique difficile a lire.
 *
 * Exemple de reponse :
 * {
 *   "timestamp": "2026-04-08T17:00:00",
 *   "status": 400,
 *   "message": "La quantite doit etre > 0",
 *   "path": "/capex/items"
 * }
 *
 * Ce DTO est dedie a l'API :
 * - il ne represente pas une table de base de donnees
 * - il ne contient que les informations utiles pour le client
 */
public class ErrorResponse {

    /**
     * Date et heure de l'erreur.
     *
     * Cela aide a relier une erreur visible dans Postman
     * avec les logs du backend.
     */
    private LocalDateTime timestamp;

    /**
     * Message lisible pour expliquer le probleme.
     */
    private String message;

    /**
     * Code HTTP retourne.
     * Exemple :
     * - 400 pour une erreur metier
     * - 500 pour une erreur technique inattendue
     */
    private int status;

    /**
     * URL appelee au moment de l'erreur.
     */
    private String path;

    public ErrorResponse() {
    }

    public ErrorResponse(LocalDateTime timestamp, String message, int status, String path) {
        this.timestamp = timestamp;
        this.message = message;
        this.status = status;
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
