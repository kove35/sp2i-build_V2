package com.sp2i.infrastructure.persistence;

import com.sp2i.domain.capex.CapexItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Ce fichier sert a acceder aux donnees CAPEX stockees en base.
 *
 * Un repository est la couche chargee de parler a la base de donnees.
 * Son role est simple :
 * - lire des donnees
 * - sauvegarder des donnees
 * - supprimer des donnees
 *
 * Ici, on etend JpaRepository.
 *
 * JpaRepository nous donne automatiquement beaucoup de methodes utiles :
 * - findAll() : lire toutes les lignes
 * - findById(id) : lire une ligne par son identifiant
 * - save(entity) : inserer ou mettre a jour
 * - deleteById(id) : supprimer
 *
 * Parametres generiques :
 * - CapexItem : le type de l'entite geree
 * - Long : le type de la cle primaire
 *
 * Comme Spring genere l'implementation automatiquement,
 * nous n'avons pas besoin d'ecrire de SQL pour ces operations simples.
 */
public interface CapexItemRepository extends JpaRepository<CapexItem, Long> {

    /**
     * Cette methode demande a Spring Data JPA de retrouver
     * tous les postes CAPEX d'un projet donne.
     *
     * Spring comprend le nom de la methode :
     * - findByProject_Id
     * - donc il fabrique automatiquement la requete SQL correspondante
     *
     * C'est un bon exemple de repository "debutant friendly" :
     * on obtient un filtrage sans ecrire de SQL a la main.
     */
    /**
     * Cette methode filtre les items a partir de l'id
     * de l'entite project liee a CapexItem.
     *
     * Pourquoi le nom contient "Project_Id" ?
     * Parce que Spring Data JPA sait naviguer dans les relations :
     * - project = l'objet lie
     * - id = le champ de cet objet
     *
     * C'est donc la version repository d'une requete du type :
     * "donne-moi tous les CapexItem dont le projet a tel id".
     */
    List<CapexItem> findByProject_Id(Long projectId);

    /**
     * Supprime tous les items CAPEX lies a un projet.
     *
     * Cette methode est utile lorsqu'on veut recharger un projet
     * avec un nouveau DQE de reference sans cumuler l'ancien jeu
     * de demonstration avec les nouvelles lignes importees.
     */
    void deleteByProject_Id(Long projectId);

    List<CapexItem> findTop10ByProject_IdOrderByIdDesc(Long projectId);

    /**
     * Retourne les 10 derniers items crees.
     *
     * Comme l'id est auto-genere et croissant,
     * un tri descendant sur l'id donne une bonne approximation
     * des lignes les plus recentes.
     */
    List<CapexItem> findTop10ByOrderByIdDesc();
}
