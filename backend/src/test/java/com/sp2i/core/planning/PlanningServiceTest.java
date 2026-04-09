package com.sp2i.core.planning;

import com.sp2i.domain.capex.CapexItem;
import com.sp2i.domain.project.CapexProject;
import com.sp2i.dto.planning.PlanningTaskDTO;
import com.sp2i.infrastructure.persistence.CapexItemRepository;
import com.sp2i.infrastructure.persistence.CapexProjectRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlanningServiceTest {

    private final CapexItemRepository capexItemRepository = mock(CapexItemRepository.class);
    private final CapexProjectRepository capexProjectRepository = mock(CapexProjectRepository.class);
    private final PlanningService planningService = new PlanningService(capexItemRepository, capexProjectRepository);

    @Test
    void shouldGeneratePlanningUsingFixedOrderAndFixedDurations() {
        CapexProject project = new CapexProject(1L, "Projet planning");

        CapexItem menuiserie = createItem(project, "Menuiserie", "Bat A", "R+1");
        CapexItem electricite = createItem(project, "Electricite", "Bat A", "RDC");
        CapexItem climatisation = createItem(project, "Climatisation", "Bat B", "R+2");
        CapexItem grosOeuvre = createItem(project, "Gros oeuvre", "Bat A", "Sous-sol");

        when(capexProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(capexItemRepository.findByProject_Id(1L))
                .thenReturn(List.of(menuiserie, electricite, climatisation, grosOeuvre));

        List<PlanningTaskDTO> tasks = planningService.generatePlanning(1L);

        assertThat(tasks).hasSize(4);

        assertThat(tasks.get(0).lot()).isEqualTo("Gros oeuvre");
        assertThat(tasks.get(0).dateDebut()).isEqualTo("jour 0");
        assertThat(tasks.get(0).dateFin()).isEqualTo("jour 3");
        assertThat(tasks.get(0).duree()).isEqualTo(3);

        assertThat(tasks.get(1).lot()).isEqualTo("Electricite");
        assertThat(tasks.get(1).dateDebut()).isEqualTo("jour 3");
        assertThat(tasks.get(1).dateFin()).isEqualTo("jour 10");
        assertThat(tasks.get(1).duree()).isEqualTo(7);

        assertThat(tasks.get(2).lot()).isEqualTo("Menuiserie");
        assertThat(tasks.get(2).dateDebut()).isEqualTo("jour 10");
        assertThat(tasks.get(2).dateFin()).isEqualTo("jour 15");
        assertThat(tasks.get(2).duree()).isEqualTo(5);

        assertThat(tasks.get(3).lot()).isEqualTo("Climatisation");
        assertThat(tasks.get(3).dateDebut()).isEqualTo("jour 15");
        assertThat(tasks.get(3).dateFin()).isEqualTo("jour 19");
        assertThat(tasks.get(3).duree()).isEqualTo(4);
    }

    private CapexItem createItem(CapexProject project, String lot, String batiment, String niveau) {
        CapexItem item = new CapexItem();
        item.setProject(project);
        item.setLot(lot);
        item.setBatiment(batiment);
        item.setNiveau(niveau);
        return item;
    }
}
