package com.sp2i.dto.dqe;

/**
 * Ce fichier sert a representer le formulaire de creation manuelle
 * d'une ligne DQE depuis le frontend React.
 *
 * Pourquoi un DTO dedie ?
 * - pour exposer un vocabulaire simple au frontend
 * - pour eviter de lier directement l'API a l'entite JPA
 * - pour pouvoir faire evoluer le formulaire sans casser la base
 */
public class CreateDqeItemRequest {

    private Long projectId;
    private String lot;
    private String famille;
    private String designation;
    private String unite;
    private String batiment;
    private String niveau;
    private Double quantite;
    private Double prixUnitaire;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getLot() {
        return lot;
    }

    public void setLot(String lot) {
        this.lot = lot;
    }

    public String getFamille() {
        return famille;
    }

    public void setFamille(String famille) {
        this.famille = famille;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getUnite() {
        return unite;
    }

    public void setUnite(String unite) {
        this.unite = unite;
    }

    public String getBatiment() {
        return batiment;
    }

    public void setBatiment(String batiment) {
        this.batiment = batiment;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public Double getQuantite() {
        return quantite;
    }

    public void setQuantite(Double quantite) {
        this.quantite = quantite;
    }

    public Double getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(Double prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }
}
