package com.sp2i.core.dqe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp2i.core.openai.OpenAIService;
import com.sp2i.dto.dqe.DqeLineAnalysisDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DqeAiServiceTest {

    private final OpenAIService openAIService = mock(OpenAIService.class);
    private final DqeAiService dqeAiService = new DqeAiService(
            new ObjectMapper(),
            new DqeSemanticHelper(),
            openAIService
    );

    @Test
    void shouldSplitFrenchAmountsWithoutCollapsingUnitPriceAndTotal() throws Exception {
        when(openAIService.callOpenAI(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new RuntimeException("fallback"));

        String text = """
                LOT 10 : PLOMBERIE SANITAIRE
                BATIMENT PRINCIPAL
                ELEVATION RDC
                3 Abonnement, mensualite concessionnaires Ens 1 2 507 000 2 507 000
                """;

        List<DqeLineAnalysisDTO> result = dqeAiService.analyze(text);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPrixUnitaire()).isEqualTo(2_507_000d);
        assertThat(result.get(0).getTotal()).isEqualTo(2_507_000d);
        assertThat(result.get(0).getQuantite()).isEqualTo(1d);
    }

    @Test
    void shouldUseReferenceStyleContextForFoundationsBlock() throws Exception {
        when(openAIService.callOpenAI(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new RuntimeException("fallback"));

        String text = """
                LOT 1 : GROS OEUVRE ET DEMOLITION
                C FONDATIONS
                BATIMENT PRINCIPAL
                1 Semelles isolees
                Beton de proprete m3 3.07 338 720 1 039 870
                Beton 400 hydr m3 4.36 189 175 824 803
                Coffrage m2 15.84 5 750 91 080
                Acier kg 436 1 380 601 680
                """;

        List<DqeLineAnalysisDTO> result = dqeAiService.analyze(text);

        assertThat(result).hasSize(4);
        assertThat(result).allSatisfy(line -> {
            assertThat(line.getLot()).isEqualTo("Gros oeuvre");
            assertThat(line.getBatiment()).isEqualTo("Bâtiment Principal");
            assertThat(line.getNiveau()).isEqualTo("FONDATIONS");
            assertThat(line.getFamille()).isEqualTo("Structure");
        });
        assertThat(result.get(0).getDesignation()).isEqualTo("Semelles isolees - Beton de proprete");
        assertThat(result.get(0).getPrixUnitaire()).isEqualTo(338_720d);
        assertThat(result.get(0).getTotal()).isEqualTo(1_039_870d);
    }

    @Test
    void shouldParseStructuredJsonReferenceDirectly() {
        String json = """
                {
                  "projet": "CENTRE MEDICAL",
                  "lots": [
                    {
                      "numero": 1,
                      "description": "GROS OEUVRE ET DEMOLITION",
                      "sections": [
                        {
                          "nom": "BATIMENT PRINCIPAL - ETAGE 1",
                          "details": [
                            {
                              "designation": "Poteaux en BA - Beton 400",
                              "unite": "m3",
                              "quantite": 2.48,
                              "prix_unitaire": 189175,
                              "total": 469154
                            },
                            {
                              "designation": "Poteaux en BA - Acier",
                              "unite": "kg",
                              "quantite": 297.6,
                              "prix_unitaire": 1380,
                              "total": 410688
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        List<DqeLineAnalysisDTO> result = dqeAiService.analyze(json);

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(line -> {
            assertThat(line.getLot()).isEqualTo("Gros oeuvre");
            assertThat(line.getBatiment()).isEqualTo("Bâtiment Principal");
            assertThat(line.getNiveau()).isEqualTo("ETAGE 1");
            assertThat(line.getPrixUnitaire()).isNotNull();
            assertThat(line.getTotal()).isNotNull();
        });
        assertThat(result.get(0).getDesignation()).isEqualTo("Poteaux en BA - Beton 400");
        assertThat(result.get(0).getFamille()).isEqualTo("Structure");
        assertThat(result.get(1).getUnite()).isEqualTo("kg");
    }

    @Test
    void shouldNotInferFakeSsLevelFromOrdinaryWords() throws Exception {
        when(openAIService.callOpenAI(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new RuntimeException("fallback"));

        String text = """
                LOT 1 : GROS OEUVRE ET DEMOLITION
                3 Abonnement, mensualite concessionnaires Ens 1 2 507 000 2 507 000
                """;

        List<DqeLineAnalysisDTO> result = dqeAiService.analyze(text);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNiveau()).isEqualTo("GLOBAL");
    }

    @Test
    void shouldParseStructuredCsvReferenceDirectly() {
        String csv = """
                Lot;Sous-lot;Batiment;Niveau;Designation;Unite;Quantite;PU;Total
                1;A - Installation generale;Chantier;Niveau Chantier;Mobilisation;Ens;1;4755250;4755250
                1;C - Fondations;Batiment Principal;RDC;Semelles isolees - Beton de proprete;m3;3,07;338720;1039870
                """;

        List<DqeLineAnalysisDTO> result = dqeAiService.analyze(csv);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getLot()).isEqualTo("Gros oeuvre");
        assertThat(result.get(0).getBatiment()).isEqualTo("Site");
        assertThat(result.get(0).getNiveau()).isEqualTo("GLOBAL");
        assertThat(result.get(0).getDesignation()).isEqualTo("Mobilisation");
        assertThat(result.get(1).getBatiment()).isEqualTo("Bâtiment Principal");
        assertThat(result.get(1).getNiveau()).isEqualTo("RDC");
        assertThat(result.get(1).getPrixUnitaire()).isEqualTo(338_720d);
        assertThat(result.get(1).getTotal()).isEqualTo(1_039_870d);
    }

    @Test
    void shouldParseStructuredTabbedReferenceDirectly() {
        String table = """
                Lot\tSous-lot\tBatiment\tNiveau\tDesignation\tUnite\tQuantite\tPU\tTotal
                1\tA - Installation generale\tChantier\tNiveau Chantier\tMobilisation\tEns\t1\t4755250\t4755250
                1\tD - Elevation RDC\tBatiment Principal\tRDC\tPoteaux en BA - Beton 400\tm3\t2,48\t189175\t469154
                """;

        List<DqeLineAnalysisDTO> result = dqeAiService.analyze(table);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDesignation()).isEqualTo("Mobilisation");
        assertThat(result.get(0).getBatiment()).isEqualTo("Site");
        assertThat(result.get(0).getNiveau()).isEqualTo("GLOBAL");
        assertThat(result.get(0).getLot()).isEqualTo("Gros oeuvre");
        assertThat(result.get(1).getDesignation()).isEqualTo("Poteaux en BA - Beton 400");
        assertThat(result.get(1).getBatiment()).isEqualTo("Bâtiment Principal");
        assertThat(result.get(1).getNiveau()).isEqualTo("RDC");
        assertThat(result.get(1).getLot()).isEqualTo("Gros oeuvre");
    }
}
