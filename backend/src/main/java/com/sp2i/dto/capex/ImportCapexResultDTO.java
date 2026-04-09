package com.sp2i.dto.capex;

/**
 * Ce fichier sert a representer la reponse JSON
 * retournee apres un import Excel.
 *
 * Pourquoi utiliser un DTO de retour ?
 * - pour renvoyer un JSON clair au frontend ou a Postman
 * - pour eviter de renvoyer directement des objets metier inutiles
 * - pour garder une API simple et stable
 *
 * Exemple de reponse :
 * {
 *   "lignesImportees": 12
 * }
 */
public class ImportCapexResultDTO {

    /**
     * Nombre de lignes effectivement importees en base.
     */
    private int lignesImportees;

    public ImportCapexResultDTO() {
    }

    public ImportCapexResultDTO(int lignesImportees) {
        this.lignesImportees = lignesImportees;
    }

    public int getLignesImportees() {
        return lignesImportees;
    }

    public void setLignesImportees(int lignesImportees) {
        this.lignesImportees = lignesImportees;
    }
}
