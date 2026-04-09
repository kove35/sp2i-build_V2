package com.sp2i.infrastructure.persistence;

import com.sp2i.domain.project.CapexProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Ce fichier sert a gerer l'acces aux projets en base de donnees.
 *
 * Le repository est la couche "infrastructure".
 * Il fait le lien entre notre code Java et PostgreSQL.
 *
 * En etendant JpaRepository, on recupere automatiquement des methodes utiles :
 * - findAll() pour lire tous les projets
 * - findById(id) pour lire un projet precis
 * - save(project) pour creer ou modifier un projet
 * - deleteById(id) pour supprimer
 *
 * Spring Data JPA genere l'implementation pour nous.
 * Cela evite d'ecrire du SQL quand les besoins sont simples.
 */
public interface CapexProjectRepository extends JpaRepository<CapexProject, Long> {

    List<CapexProject> findByOwner_Id(Long ownerId);
}
