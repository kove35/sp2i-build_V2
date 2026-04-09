package com.sp2i.dto.dqe;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO mutable qui represente une ligne DQE pendant tout le pipeline.
 */
public class DqeLineAnalysisDTO {

    private String designation;
    private Double quantite;
    private String unite;
    private Double prixUnitaire;
    private Double total;
    private String lot;
    private String famille;
    private String batiment;
    private String niveau;
    private List<String> erreurs = new ArrayList<>();
    private boolean valide;
    private Double scoreQualite;
    private Double prixLocalEstime;
    private Double prixImportEstime;
    private Double prixImportRendu;
    private String decision;
    private String risque;
    private String fournisseurSuggestion;
    private Double scoreConfiance;

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public Double getQuantite() { return quantite; }
    public void setQuantite(Double quantite) { this.quantite = quantite; }
    public String getUnite() { return unite; }
    public void setUnite(String unite) { this.unite = unite; }
    public Double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(Double prixUnitaire) { this.prixUnitaire = prixUnitaire; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }
    public String getLot() { return lot; }
    public void setLot(String lot) { this.lot = lot; }
    public String getFamille() { return famille; }
    public void setFamille(String famille) { this.famille = famille; }
    public String getBatiment() { return batiment; }
    public void setBatiment(String batiment) { this.batiment = batiment; }
    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }
    public List<String> getErreurs() { return erreurs; }
    public void setErreurs(List<String> erreurs) { this.erreurs = erreurs; }
    public boolean isValide() { return valide; }
    public void setValide(boolean valide) { this.valide = valide; }
    public Double getScoreQualite() { return scoreQualite; }
    public void setScoreQualite(Double scoreQualite) { this.scoreQualite = scoreQualite; }
    public Double getPrixLocalEstime() { return prixLocalEstime; }
    public void setPrixLocalEstime(Double prixLocalEstime) { this.prixLocalEstime = prixLocalEstime; }
    public Double getPrixImportEstime() { return prixImportEstime; }
    public void setPrixImportEstime(Double prixImportEstime) { this.prixImportEstime = prixImportEstime; }
    public Double getPrixImportRendu() { return prixImportRendu; }
    public void setPrixImportRendu(Double prixImportRendu) { this.prixImportRendu = prixImportRendu; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getRisque() { return risque; }
    public void setRisque(String risque) { this.risque = risque; }
    public String getFournisseurSuggestion() { return fournisseurSuggestion; }
    public void setFournisseurSuggestion(String fournisseurSuggestion) { this.fournisseurSuggestion = fournisseurSuggestion; }
    public Double getScoreConfiance() { return scoreConfiance; }
    public void setScoreConfiance(Double scoreConfiance) { this.scoreConfiance = scoreConfiance; }
}
