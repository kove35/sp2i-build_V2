package com.sp2i.core.capex;

import com.sp2i.core.exception.BusinessException;
import com.sp2i.domain.capex.CapexItem;
import com.sp2i.domain.project.CapexProject;
import com.sp2i.dto.capex.CreateCapexItemRequest;
import com.sp2i.infrastructure.persistence.CapexItemRepository;
import com.sp2i.infrastructure.persistence.CapexProjectRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CapexServiceTest {

    private final CapexItemRepository capexItemRepository = mock(CapexItemRepository.class);
    private final CapexProjectRepository capexProjectRepository = mock(CapexProjectRepository.class);
    private final ImportCostCalculator importCostCalculator =
            new ImportCostCalculator(0.08d, 0.02d, 0.15d, 0.05d, 0.05d, 0.10d);
    private final CapexService capexService =
            new CapexService(capexItemRepository, capexProjectRepository, importCostCalculator);

    @Test
    void shouldCalculateCapexSummary() {
        CapexItem first = createItem("LOT-A", "ELEC", 10d, 100d, 40d);
        CapexItem second = createItem("LOT-B", "CVC", 5d, 50d, 20d);

        double capexBrut = capexService.calculCapexBrut(List.of(first, second));
        double capexOptimise = capexService.calculCapexOptimise(List.of(first, second));
        double economie = capexService.calculEconomie(List.of(first, second));
        double taux = capexService.tauxOptimisation(List.of(first, second));

        assertThat(capexBrut).isEqualTo(1250d);
        assertThat(capexOptimise).isCloseTo(768.180105d, offset(0.0001d));
        assertThat(economie).isCloseTo(481.819895d, offset(0.0001d));
        assertThat(taux).isCloseTo(0.385455916d, offset(0.0001d));
    }

    @Test
    void shouldReturnZeroRateWhenCapexBrutIsZero() {
        CapexItem item = createItem("LOT-A", "ELEC", 0d, 100d, 80d);

        assertThat(capexService.tauxOptimisation(List.of(item))).isZero();
    }

    @Test
    void shouldCalculateGainAndMissingChinaIndicators() {
        CapexItem importableItem = createItem("LOT-A", "ELEC", 10d, 100d, 40d);
        CapexItem missingChinaItem = createItem("LOT-B", "CVC", 5d, 50d, null);

        assertThat(capexService.calculGainTotal(List.of(importableItem, missingChinaItem)))
                .isCloseTo(385.455916d, offset(0.0001d));
        assertThat(capexService.countItemsWithoutImportPrice(List.of(importableItem, missingChinaItem))).isEqualTo(1);
        assertThat(capexService.calculCapexSansPrixChine(List.of(importableItem, missingChinaItem))).isEqualTo(250d);
    }

    @Test
    void shouldCreateCapexItemWhenRequestIsValid() {
        CreateCapexItemRequest request = new CreateCapexItemRequest();
        request.setProjectId(1L);
        request.setLot("LOT-A");
        request.setQuantite(3d);
        request.setCoutLocal(100d);
        request.setCoutImport(90d);

        CapexProject project = new CapexProject(1L, "Projet test");

        when(capexProjectRepository.findById(1L)).thenReturn(java.util.Optional.of(project));
        when(capexItemRepository.save(org.mockito.ArgumentMatchers.any(CapexItem.class)))
                .thenAnswer(invocation -> {
                    CapexItem item = invocation.getArgument(0);
                    item.setId(10L);
                    return item;
                });

        CapexItem savedItem = capexService.createCapexItem(request);

        assertThat(savedItem.getId()).isEqualTo(10L);
        assertThat(savedItem.getProjectId()).isEqualTo(1L);
        assertThat(savedItem.getProject()).isEqualTo(project);
        assertThat(savedItem.getLot()).isEqualTo("LOT-A");
        assertThat(savedItem.getCoutLocal()).isEqualTo(100d);
    }

    @Test
    void shouldFilterSummaryByLot() {
        CapexItem first = createItem("LOT-A", "ELEC", 10d, 100d, 40d);
        CapexItem second = createItem("LOT-B", "CVC", 5d, 50d, 20d);

        when(capexItemRepository.findAll()).thenReturn(List.of(first, second));

        assertThat(capexService.getSummary("LOT-A", null, null, null).capexBrut()).isEqualTo(1000d);
        assertThat(capexService.getSummary("LOT-A", null, null, null).capexOptimise())
                .isCloseTo(614.544084d, offset(0.0001d));
        assertThat(capexService.getSummary("LOT-A", null, null, null).gainTotal())
                .isCloseTo(385.455916d, offset(0.0001d));
    }

    @Test
    void shouldSimulateProjectScenarios() {
        CapexProject project = new CapexProject(1L, "Projet scenario");
        CapexItem first = createItem("LOT-A", "ELEC", 10d, 100d, 40d);
        first.setProject(project);
        CapexItem second = createItem("LOT-B", "CVC", 5d, 50d, 20d);
        second.setProject(project);

        when(capexProjectRepository.findById(1L)).thenReturn(java.util.Optional.of(project));
        when(capexItemRepository.findByProject_Id(1L)).thenReturn(List.of(first, second));

        var simulation = capexService.simulateScenarios(1L);

        assertThat(simulation.capexLocal()).isEqualTo(1250d);
        assertThat(simulation.capexImport()).isCloseTo(768.180105d, offset(0.0001d));
        assertThat(simulation.capexOptimise()).isCloseTo(768.180105d, offset(0.0001d));
        assertThat(simulation.gainImport()).isCloseTo(481.819895d, offset(0.0001d));
        assertThat(simulation.gainOptimise()).isCloseTo(481.819895d, offset(0.0001d));
    }

    @Test
    void shouldRefuseCapexItemCreationWhenQuantiteIsInvalid() {
        CreateCapexItemRequest request = new CreateCapexItemRequest();
        request.setProjectId(1L);
        request.setQuantite(0d);
        request.setCoutLocal(100d);

        assertThatThrownBy(() -> capexService.createCapexItem(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("La quantite doit etre > 0");
    }

    private CapexItem createItem(String lot, String famille, Double quantite, Double coutLocal, Double coutImport) {
        CapexItem item = new CapexItem();
        item.setLot(lot);
        item.setFamille(famille);
        item.setQuantite(quantite);
        item.setCoutLocal(coutLocal);
        item.setCoutImport(coutImport);
        return item;
    }
}
