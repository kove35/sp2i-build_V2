package com.sp2i.dto.capex;

import com.sp2i.domain.capex.PrioriteExecution;

import java.util.List;

/**
 * Ce fichier sert a representer le corps JSON envoye pour creer un poste CAPEX.
 *
 * Exemple de JSON attendu :
 * {
 *   "projectId": 1,
 *   "lot": "Electricite",
 *   "famille": "Tableau",
 *   "batiment": "Bat A",
 *   "niveau": "R+1",
 *   "quantite": 10,
 *   "coutLocal": 1500,
 *   "coutImport": 1200
 * }
 *
 * Ce DTO est volontairement simple et lisible pour un debutant.
 */
public class CreateCapexItemRequest {

    /**
     * Identifiant du projet auquel rattacher le poste CAPEX.
     */
    private Long projectId;

    /**
     * Lot principal du poste.
     */
    private String lot;

    /**
     * Famille plus detaillee du poste.
     */
    private String famille;

    /**
     * Batiment concerne.
     */
    private String batiment;

    /**
     * Niveau ou etage concerne.
     */
    private String niveau;

    /**
     * Quantite a acheter ou a installer.
     */
    private Double quantite;

    /**
     * Cout local unitaire.
     */
    private Double coutLocal;

    /**
     * Cout import unitaire.
     */
    private Double coutImport;

    /**
     * Duree estimee du lot en jours.
     */
    private Integer dureeEstimee;

    /**
     * Ordre initial eventuel.
     *
     * Si ce champ n'est pas renseigne,
     * le PlanningService pourra le recalculer plus tard.
     */
    private Integer ordreExecution;

    /**
     * Priorite de la tache.
     */
    private PrioriteExecution priorite;

    /**
     * Liste des lots prealables.
     */
    private List<String> dependances;

    public CreateCapexItemRequest() {
    }

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

    public Double getCoutLocal() {
        return coutLocal;
    }

    public void setCoutLocal(Double coutLocal) {
        this.coutLocal = coutLocal;
    }

    public Double getCoutImport() {
        return coutImport;
    }

    public void setCoutImport(Double coutImport) {
        this.coutImport = coutImport;
    }

    public Integer getDureeEstimee() {
        return dureeEstimee;
    }

    public void setDureeEstimee(Integer dureeEstimee) {
        this.dureeEstimee = dureeEstimee;
    }

    public Integer getOrdreExecution() {
        return ordreExecution;
    }

    public void setOrdreExecution(Integer ordreExecution) {
        this.ordreExecution = ordreExecution;
    }

    public PrioriteExecution getPriorite() {
        return priorite;
    }

    public void setPriorite(PrioriteExecution priorite) {
        this.priorite = priorite;
    }

    public List<String> getDependances() {
        return dependances;
    }

    public void setDependances(List<String> dependances) {
        this.dependances = dependances;
    }
}
