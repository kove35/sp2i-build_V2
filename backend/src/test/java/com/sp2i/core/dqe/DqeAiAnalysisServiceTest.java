package com.sp2i.core.dqe;

import com.sp2i.dto.dqe.DqeAiAnalysisResultDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DqeAiAnalysisServiceTest {

    private final DqeImportService dqeImportService = mock(DqeImportService.class);
    private final DqeAiAnalysisService dqeAiAnalysisService = new DqeAiAnalysisService(
            dqeImportService,
            new DqeSemanticHelper()
    );

    @Test
    void shouldAnalyzeDocumentWithAiEnrichment() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dqe-demo.pdf",
                "application/pdf",
                "demo".getBytes()
        );

        when(dqeImportService.extractTextContent(file)).thenReturn("dummy text");
        when(dqeImportService.normalizeDocumentLines("dummy text")).thenReturn(List.of(
                "Fenetre aluminium 12 500",
                "Tableau electrique principal 2",
                "Article libre"
        ));

        DqeAiAnalysisResultDTO result = dqeAiAnalysisService.analyze(file);

        assertThat(result.scoreGlobal()).isBetween(15d, 100d);
        assertThat(result.lignesAnalysees()).isEqualTo(3);
        assertThat(result.lignes()).hasSize(3);
        assertThat(result.lignes().get(0).lot()).isEqualTo(DqeSemanticHelper.LOT_MENUISERIE_ALU);
        assertThat(result.lignes().get(0).prixImportEstime()).isNotNull();
        assertThat(result.lignes().get(0).decision()).isIn("IMPORT", "LOCAL", "MIX");
        assertThat(result.lignes().get(1).niveauRisque()).isEqualTo("ELEVE");
        assertThat(result.lignes().get(2).alertes()).contains("Classification incertaine");
    }
}
