package com.sp2i.core.dqe;

import com.sp2i.core.exception.BusinessException;
import com.sp2i.domain.capex.CapexItem;
import com.sp2i.domain.project.CapexProject;
import com.sp2i.dto.dqe.CreateDqeItemRequest;
import com.sp2i.infrastructure.persistence.CapexItemRepository;
import com.sp2i.infrastructure.persistence.CapexProjectRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DqeServiceTest {

    private final CapexItemRepository capexItemRepository = mock(CapexItemRepository.class);
    private final CapexProjectRepository capexProjectRepository = mock(CapexProjectRepository.class);
    private final DqeService dqeService = new DqeService(capexItemRepository, capexProjectRepository);

    @Test
    void shouldCreateDqeItemAsCapexItem() {
        CreateDqeItemRequest request = new CreateDqeItemRequest();
        request.setProjectId(1L);
        request.setLot("Menuiserie");
        request.setFamille("Fenetres");
        request.setDesignation("Fenetre aluminium");
        request.setUnite("U");
        request.setQuantite(2d);
        request.setPrixUnitaire(500d);

        CapexProject project = new CapexProject(1L, "Projet DQE");

        when(capexProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(capexItemRepository.save(any(CapexItem.class))).thenAnswer(invocation -> {
            CapexItem item = invocation.getArgument(0);
            item.setId(10L);
            return item;
        });

        CapexItem savedItem = dqeService.createDqeItem(request);

        assertThat(savedItem.getId()).isEqualTo(10L);
        assertThat(savedItem.getProject()).isEqualTo(project);
        assertThat(savedItem.getDesignation()).isEqualTo("Fenetre aluminium");
        assertThat(savedItem.getUnite()).isEqualTo("U");
        assertThat(savedItem.getPrixUnitaire()).isEqualTo(500d);
        assertThat(savedItem.getPrixTotal()).isEqualTo(1000d);
    }

    @Test
    void shouldRefuseDqeItemWhenPriceIsMissing() {
        CreateDqeItemRequest request = new CreateDqeItemRequest();
        request.setProjectId(1L);
        request.setLot("Menuiserie");
        request.setDesignation("Fenetre aluminium");
        request.setQuantite(2d);

        assertThatThrownBy(() -> dqeService.createDqeItem(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Le prix unitaire est obligatoire");
    }

    @Test
    void shouldMixDefaultAndObservedFamilySuggestions() {
        CapexItem item = new CapexItem();
        item.setLot("Menuiserie");
        item.setFamille("Cloisons vitrées");

        when(capexItemRepository.findAll()).thenReturn(List.of(item));

        var suggestions = dqeService.getFamilySuggestions("Menuiserie");

        assertThat(suggestions.familles()).contains("Fenetres", "Portes", "Volets", "Cloisons vitrées");
    }
}
