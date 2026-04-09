package com.sp2i.domain.project;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sp2i.domain.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Ce fichier sert a representer un projet CAPEX dans l'application.
 *
 * Un projet porte :
 * - les informations generales
 * - les parametres logistiques
 * - les parametres metier
 * - la structure immobiliere serializee en JSON
 *
 * Cette entite reste volontairement simple a lire pour un debutant.
 */
@Entity
@Table(name = "capex_project")
public class CapexProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String location;
    private String type;
    private Double surface;
    private Double budget;

    /**
     * Code devise ISO du projet.
     *
     * Exemples :
     * - XAF pour le franc CFA
     * - EUR pour l'euro
     * - USD pour le dollar
     *
     * On garde un code court plutot qu'un texte libre,
     * car cela facilite :
     * - l'affichage cote frontend
     * - les calculs multi-projets
     * - les futures integrations finance
     */
    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "transport_rate")
    private Double transportRate;

    @Column(name = "douane_rate")
    private Double douaneRate;

    @Column(name = "port_rate")
    private Double portRate;

    @Column(name = "local_rate")
    private Double localRate;

    @Column(name = "margin_rate")
    private Double marginRate;

    @Column(name = "risk_rate")
    private Double riskRate;

    @Column(name = "import_threshold")
    private Double importThreshold;

    @Column(name = "strategy_mode")
    private String strategyMode;

    /**
     * Structure immobiliere stockee en JSON texte.
     *
     * Pourquoi une simple String ?
     * - c'est facile a stocker dans PostgreSQL
     * - on garde la flexibilite du format JSON
     * - le service convertit l'objet Java en texte et inversement
     */
    @Column(name = "structure_json")
    private String structureJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private AppUser owner;

    public CapexProject() {
    }

    public CapexProject(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getStructureJson() {
        return structureJson;
    }

    public void setStructureJson(String structureJson) {
        this.structureJson = structureJson;
    }

    public AppUser getOwner() {
        return owner;
    }

    public void setOwner(AppUser owner) {
        this.owner = owner;
    }

    public Long getUserId() {
        return owner == null ? null : owner.getId();
    }
}
