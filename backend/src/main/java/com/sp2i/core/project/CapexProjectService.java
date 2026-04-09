package com.sp2i.core.project;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp2i.core.exception.BusinessException;
import com.sp2i.domain.project.CapexProject;
import com.sp2i.domain.user.AppUser;
import com.sp2i.dto.project.CreateProjectRequest;
import com.sp2i.dto.project.ProjectStructureResponse;
import com.sp2i.infrastructure.persistence.AppUserRepository;
import com.sp2i.infrastructure.persistence.CapexProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Ce fichier sert a porter la logique metier autour des projets CAPEX.
 *
 * Le service gere maintenant :
 * - la creation complete d'un projet SaaS
 * - la conversion de la structure immobiliere en JSON
 * - la lecture de cette structure pour le frontend
 */
@Service
public class CapexProjectService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CapexProjectService.class);

    private final CapexProjectRepository capexProjectRepository;
    private final AppUserRepository appUserRepository;
    private final ObjectMapper objectMapper;

    public CapexProjectService(
            CapexProjectRepository capexProjectRepository,
            AppUserRepository appUserRepository,
            ObjectMapper objectMapper
    ) {
        this.capexProjectRepository = capexProjectRepository;
        this.appUserRepository = appUserRepository;
        this.objectMapper = objectMapper;
    }

    public List<CapexProject> findAll(Long userId) {
        LOGGER.info("Lecture de la liste des projets CAPEX pour userId={}", userId);
        List<CapexProject> projects = userId == null
                ? capexProjectRepository.findAll()
                : capexProjectRepository.findByOwner_Id(userId);
        LOGGER.info("{} projet(s) trouve(s)", projects.size());
        return projects;
    }

    /**
     * Cree un nouveau projet complet a partir du DTO HTTP.
     */
    public CapexProject createProject(CreateProjectRequest request, Long userId) {
        if (request == null) {
            throw new BusinessException("La requete projet est obligatoire.");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException("Le nom du projet est obligatoire.");
        }

        LOGGER.info("Creation projet : {}", request.getName());

        CapexProject project = new CapexProject();
        project.setName(request.getName().trim());
        project.setLocation(trimToNull(request.getLocation()));
        project.setType(trimToNull(request.getType()));
        project.setSurface(request.getSurface());
        project.setBudget(request.getBudget());
        project.setCurrencyCode(normalizeCurrencyCode(request.getCurrencyCode()));
        project.setTransportRate(request.getTransportRate());
        project.setDouaneRate(request.getDouaneRate());
        project.setPortRate(request.getPortRate());
        project.setLocalRate(request.getLocalRate());
        project.setMarginRate(request.getMarginRate());
        project.setRiskRate(request.getRiskRate());
        project.setImportThreshold(request.getImportThreshold());
        project.setStrategyMode(trimToNull(request.getStrategyMode()));
        project.setStructureJson(writeStructureJson(request.getStructure()));

        if (userId != null) {
            AppUser owner = appUserRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException("Utilisateur introuvable"));
            project.setOwner(owner);
        }

        CapexProject savedProject = capexProjectRepository.save(project);
        LOGGER.info("Projet cree avec succes : id={}, name={}", savedProject.getId(), savedProject.getName());
        return savedProject;
    }

    /**
     * Relit la structure immobiliere en objet Java lisible.
     */
    public ProjectStructureResponse getProjectStructure(Long projectId, Long userId) {
        CapexProject project = findProjectAccessibleByUser(projectId, userId);

        if (project.getStructureJson() == null || project.getStructureJson().isBlank()) {
            return new ProjectStructureResponse(List.of());
        }

        try {
            return objectMapper.readValue(project.getStructureJson(), ProjectStructureResponse.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Impossible de relire la structure du projet");
        }
    }

    private CapexProject findProjectAccessibleByUser(Long projectId, Long userId) {
        CapexProject project = capexProjectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("Projet introuvable"));

        if (userId != null && project.getUserId() != null && !project.getUserId().equals(userId)) {
            throw new BusinessException("Projet introuvable");
        }

        return project;
    }

    private String writeStructureJson(CreateProjectRequest.ProjectStructureRequest structure) {
        if (structure == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(structure);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Impossible de sauvegarder la structure du projet");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * Normalise la devise d'un projet.
     *
     * Choix metier pour SP2I :
     * - si le frontend n'envoie rien, on prend XAF
     * - cela correspond a la devise de demonstration en franc CFA
     * - on stocke toujours un code en majuscules pour rester coherent
     */
    private String normalizeCurrencyCode(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return "XAF";
        }
        return currencyCode.trim().toUpperCase();
    }
}
