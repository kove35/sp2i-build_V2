package com.sp2i.dto.project;

import java.util.List;

/**
 * Ce fichier sert a representer la requete HTTP de creation de projet.
 *
 * On y place :
 * - les informations generales du projet
 * - les parametres logistiques
 * - les parametres metier
 * - la structure immobiliere
 *
 * L'idee est de donner au frontend un contrat API clair,
 * sans exposer directement l'entite JPA.
 */
public class CreateProjectRequest {

    private String name;
    private String location;
    private String type;
    private Double surface;
    private Double budget;
    private String currencyCode;
    private Double transportRate;
    private Double douaneRate;
    private Double portRate;
    private Double localRate;
    private Double marginRate;
    private Double riskRate;
    private Double importThreshold;
    private String strategyMode;
    private ProjectStructureRequest structure;

    public CreateProjectRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getSurface() {
        return surface;
    }

    public void setSurface(Double surface) {
        this.surface = surface;
    }

    public Double getBudget() {
        return budget;
    }

    public void setBudget(Double budget) {
        this.budget = budget;
    }

    /**
     * Code devise choisi pour le projet.
     *
     * On utilise un code ISO simple comme XAF, EUR ou USD.
     * Cela evite les ambiguities de libelles du type
     * "franc", "franc CFA", "CFA", etc.
     */
    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Double getTransportRate() {
        return transportRate;
    }

    public void setTransportRate(Double transportRate) {
        this.transportRate = transportRate;
    }

    public Double getDouaneRate() {
        return douaneRate;
    }

    public void setDouaneRate(Double douaneRate) {
        this.douaneRate = douaneRate;
    }

    public Double getPortRate() {
        return portRate;
    }

    public void setPortRate(Double portRate) {
        this.portRate = portRate;
    }

    public Double getLocalRate() {
        return localRate;
    }

    public void setLocalRate(Double localRate) {
        this.localRate = localRate;
    }

    public Double getMarginRate() {
        return marginRate;
    }

    public void setMarginRate(Double marginRate) {
        this.marginRate = marginRate;
    }

    public Double getRiskRate() {
        return riskRate;
    }

    public void setRiskRate(Double riskRate) {
        this.riskRate = riskRate;
    }

    public Double getImportThreshold() {
        return importThreshold;
    }

    public void setImportThreshold(Double importThreshold) {
        this.importThreshold = importThreshold;
    }

    public String getStrategyMode() {
        return strategyMode;
    }

    public void setStrategyMode(String strategyMode) {
        this.strategyMode = strategyMode;
    }

    public ProjectStructureRequest getStructure() {
        return structure;
    }

    public void setStructure(ProjectStructureRequest structure) {
        this.structure = structure;
    }

    /**
     * Objet enfant qui decrit la structure immobiliere.
     *
     * Exemple :
     * {
     *   "batiments": [
     *     { "nom": "Bat A", "etages": ["RDC", "R+1"] }
     *   ]
     * }
     */
    public static class ProjectStructureRequest {
        private List<BuildingRequest> batiments;

        public List<BuildingRequest> getBatiments() {
            return batiments;
        }

        public void setBatiments(List<BuildingRequest> batiments) {
            this.batiments = batiments;
        }
    }

    public static class BuildingRequest {
        private String nom;
        private List<String> etages;

        public String getNom() {
            return nom;
        }

        public void setNom(String nom) {
            this.nom = nom;
        }

        public List<String> getEtages() {
            return etages;
        }

        public void setEtages(List<String> etages) {
            this.etages = etages;
        }
    }
}
