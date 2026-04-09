package com.sp2i.api;

import com.sp2i.core.project.CapexProjectService;
import com.sp2i.domain.project.CapexProject;
import com.sp2i.dto.project.CreateProjectRequest;
import com.sp2i.dto.project.ProjectStructureResponse;
import com.sp2i.infrastructure.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Ce controller expose les operations projet au frontend React.
 *
 * On y trouve :
 * - la liste des projets
 * - la creation complete d'un projet
 * - la lecture de la structure immobiliere
 */
@RestController
public class ProjectController {

    private final CapexProjectService capexProjectService;

    public ProjectController(CapexProjectService capexProjectService) {
        this.capexProjectService = capexProjectService;
    }

    @GetMapping("/projects")
    public List<CapexProject> getProjects(Authentication authentication) {
        return capexProjectService.findAll(extractCurrentUserId(authentication));
    }

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public CapexProject createProject(@RequestBody CreateProjectRequest request, Authentication authentication) {
        return capexProjectService.createProject(request, extractCurrentUserId(authentication));
    }

    /**
     * Endpoint pedagogique pour relire seulement la structure immobiliere.
     *
     * C'est utile si le frontend veut recharger les batiments/etages
     * sans relire toute la fiche projet.
     */
    @GetMapping("/projects/{id}/structure")
    public ProjectStructureResponse getProjectStructure(@PathVariable Long id, Authentication authentication) {
        return capexProjectService.getProjectStructure(id, extractCurrentUserId(authentication));
    }

    private Long extractCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return null;
        }
        return user.getId();
    }
}
