package com.sp2i.domain.capex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sp2i.domain.project.CapexProject;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.List;

/**
 * Ce fichier sert a representer un poste CAPEX dans notre application.
 *
 * Une entite JPA est une classe Java qui sera reliee a une table SQL.
 * Ici, un objet {@code CapexItem} correspond a une ligne de la table
 * {@code capex_item} dans PostgreSQL.
 *
 * Pourquoi separer l'entite du reste ?
 * - l'entite represente les donnees "brutes" du domaine metier
 * - elle ne contient pas les calculs complexes
 * - elle peut etre reutilisee par plusieurs services
 */
@Entity
@Table(name = "capex_item")
public class CapexItem {

    /**
     * Identifiant technique unique de la ligne en base.
     *
     * @Id indique a JPA que ce champ est la cle primaire.
     * @GeneratedValue indique que la base genere automatiquement la valeur.
     * GenerationType.IDENTITY convient bien avec PostgreSQL lorsque l'id est auto-incremente.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Projet parent auquel appartient ce poste CAPEX.
     *
     * Ici, on ne stocke plus seulement un identifiant numerique.
     * On declare une vraie relation JPA entre deux entites :
     * - plusieurs CapexItem peuvent appartenir a un meme CapexProject
     * - c'est donc une relation ManyToOne
     *
     * @JoinColumn(name = "project_id") indique que la cle etrangere
     * en base s'appelle project_id.
     *
     * FetchType.LAZY veut dire :
     * - JPA ne charge le projet complet que si on en a besoin
     * - cela evite de charger trop de donnees inutilement
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private CapexProject project;

    /**
     * Le lot correspond a une grande categorie de travaux ou d'achat.
     * Exemple : electricite, plomberie, structure.
     */
    private String lot;

    /**
     * La famille permet un classement plus fin que le lot.
     * Exemple : tableau electrique, cable, climatisation.
     */
    private String famille;

    /**
     * La designation correspond au libelle detaille de la ligne DQE.
     *
     * Exemple :
     * - Fenetre aluminium 120x140
     * - Tableau electrique principal
     *
     * Ce champ est tres utile pour :
     * - la saisie manuelle du DQE
     * - l'import PDF / image
     * - l'export Excel
     */
    private String designation;

    /**
     * Unite de mesure de la ligne DQE.
     *
     * Exemple :
     * - U
     * - m2
     * - ml
     */
    private String unite;

    /**
     * Le batiment permet de savoir dans quel batiment se situe le poste.
     */
    private String batiment;

    /**
     * Le niveau correspond par exemple a un etage ou une zone.
     */
    private String niveau;

    /**
     * Quantite achetee ou installee.
     *
     * Pourquoi Double ?
     * - parce qu'une quantite peut etre non entiere (exemple : 2.5 m2, 12.75 ml)
     * - cela simplifie les premiers calculs pour une application pedagogique
     *
     * Dans une application financiere tres stricte, on preferera souvent BigDecimal.
     */
    private Double quantite;

    /**
     * Cout local unitaire.
     *
     * On mappe explicitement vers la colonne cout_local.
     */
    @Column(name = "cout_local")
    private Double coutLocal;

    /**
     * Cout import unitaire.
     *
     * Le service metier comparera cout local et cout import
     * pour calculer un CAPEX optimise.
     */
    @Column(name = "cout_import")
    private Double coutImport;

    /**
     * Duree estimee en jours pour realiser la tache sur le chantier.
     *
     * Exemple :
     * - 5 jours pour une preparation simple
     * - 30 jours pour un lot plus lourd
     */
    @Column(name = "duree_estimee")
    private Integer dureeEstimee;

    /**
     * Ordre de passage calcule dans le planning.
     *
     * Ce champ peut etre renseigne par le PlanningService
     * pour memoriser une proposition d'ordonnancement.
     */
    @Column(name = "ordre_execution")
    private Integer ordreExecution;

    /**
     * Priorite metier du poste.
     *
     * @Enumerated(EnumType.STRING) permet de stocker
     * HIGH / MEDIUM / LOW directement en base.
     */
    @Enumerated(EnumType.STRING)
    private PrioriteExecution priorite;

    /**
     * Liste des lots prealables necessaires avant execution.
     *
     * Exemple :
     * - "Gros oeuvre"
     * - "Reseaux"
     *
     * Cette liste est stockee dans une colonne texte grace
     * au converter LotDependencyListConverter.
     */
    @Convert(converter = LotDependencyListConverter.class)
    private List<String> dependances;

    /**
     * Constructeur vide obligatoire pour JPA.
     *
     * JPA cree les objets en arriere-plan, donc il a besoin
     * d'un constructeur sans argument.
     */
    public CapexItem() {
    }

    /**
     * Retourne l'identifiant technique.
     */
    public Long getId() {
        return id;
    }

    /**
     * Modifie l'identifiant technique.
     *
     * En pratique, c'est souvent la base qui le genere.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Retourne le projet parent.
     */
    public CapexProject getProject() {
        return project;
    }

    /**
     * Definit le projet parent.
     */
    public void setProject(CapexProject project) {
        this.project = project;
    }

    /**
     * Retourne l'id du projet parent.
     *
     * Cette methode reste tres pratique pour :
     * - les reponses JSON simples
     * - le frontend existant
     * - la lisibilite pour un debutant
     *
     * Meme si l'entite stocke maintenant un objet CapexProject,
     * on peut toujours exposer facilement son identifiant.
     */
    public Long getProjectId() {
        if (project == null) {
            return null;
        }
        return project.getId();
    }

    /**
     * Retourne le lot.
     */
    public String getLot() {
        return lot;
    }

    /**
     * Definit le lot.
     */
    public void setLot(String lot) {
        this.lot = lot;
    }

    /**
     * Retourne la famille.
     */
    public String getFamille() {
        return famille;
    }

    /**
     * Definit la famille.
     */
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

    /**
     * Retourne le batiment.
     */
    public String getBatiment() {
        return batiment;
    }

    /**
     * Definit le batiment.
     */
    public void setBatiment(String batiment) {
        this.batiment = batiment;
    }

    /**
     * Retourne le niveau.
     */
    public String getNiveau() {
        return niveau;
    }

    /**
     * Definit le niveau.
     */
    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    /**
     * Retourne la quantite.
     */
    public Double getQuantite() {
        return quantite;
    }

    /**
     * Definit la quantite.
     */
    public void setQuantite(Double quantite) {
        this.quantite = quantite;
    }

    /**
     * Retourne le cout local.
     */
    public Double getCoutLocal() {
        return coutLocal;
    }

    /**
     * Definit le cout local.
     */
    public void setCoutLocal(Double coutLocal) {
        this.coutLocal = coutLocal;
    }

    /**
     * Getter pedagogique pour le prix unitaire DQE.
     *
     * Ici, nous reutilisons coutLocal comme prix unitaire de reference
     * pour eviter de dupliquer une information identique.
     */
    public Double getPrixUnitaire() {
        return coutLocal;
    }

    /**
     * Setter pedagogique pour le prix unitaire DQE.
     *
     * Le DQE manipule la notion de "prix unitaire".
     * Dans notre modele CAPEX existant, cette valeur correspond
     * au cout local unitaire.
     */
    public void setPrixUnitaire(Double prixUnitaire) {
        this.coutLocal = prixUnitaire;
    }

    /**
     * Prix total calcule cote objet.
     *
     * Formule :
     * prixTotal = quantite * prixUnitaire
     *
     * On ne le stocke pas en base pour eviter la redondance.
     * On le recalcule a la demande.
     */
    public Double getPrixTotal() {
        return safeValue(quantite) * safeValue(getPrixUnitaire());
    }

    /**
     * Retourne le cout import.
     */
    public Double getCoutImport() {
        return coutImport;
    }

    /**
     * Definit le cout import.
     */
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
        return dependances == null ? List.of() : dependances;
    }

    public void setDependances(List<String> dependances) {
        this.dependances = dependances;
    }

    /**
     * Gain immediat theorique.
     *
     * Formule :
     * gain = coutLocal - coutImport
     *
     * Si le prix Chine n'est pas connu, on retourne 0
     * car aucun gain fiable ne peut etre annonce.
     */
    public Double getGain() {
        if (coutImport == null) {
            return 0d;
        }
        return safeValue(coutLocal) - safeValue(coutImport);
    }

    /**
     * Taux d'economie unitaire.
     *
     * Exemple :
     * - cout local = 100
     * - cout import = 70
     * - gain = 30
     * - taux = 30 / 100 = 30%
     */
    public Double getTauxEconomie() {
        double localValue = safeValue(coutLocal);
        if (localValue == 0d || coutImport == null) {
            return 0d;
        }
        return getGain() / localValue;
    }

    /**
     * Decision metier automatisee.
     *
     * Regle :
     * - si l'import est moins cher ET que le gain depasse 20%
     *   alors on conseille IMPORT
     * - sinon on conseille LOCAL
     */
    public String getDecision() {
        if (coutImport != null && coutImport < safeValue(coutLocal) && getTauxEconomie() > 0.2d) {
            return "IMPORT";
        }
        return "LOCAL";
    }

    /**
     * Statut de comparaison avec la Chine.
     *
     * Il aide le frontend a afficher les lignes
     * qui ne sont pas encore optimisables.
     */
    public String getStatutPrixChine() {
        return coutImport == null ? "MANQUANT_CHINE" : "COMPLET";
    }

    private double safeValue(Double value) {
        return value == null ? 0d : value;
    }
}
