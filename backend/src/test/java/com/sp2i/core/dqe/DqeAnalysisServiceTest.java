package com.sp2i.core.dqe;

import com.sp2i.dto.dqe.DqeAnalysisResultDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DqeAnalysisServiceTest {

    private final DqeImportService dqeImportService = mock(DqeImportService.class);
    private final DqeAnalysisService dqeAnalysisService = new DqeAnalysisService(
            dqeImportService,
            new DqeSemanticHelper()
    );

    @Test
    void shouldAnalyzeDocumentAndComputeCountersAndScore() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dqe-demo.pdf",
                "application/pdf",
                "demo".getBytes()
        );

        when(dqeImportService.extractTextContent(file)).thenReturn("dummy text");
        when(dqeImportService.normalizeDocumentLines("dummy text")).thenReturn(List.of(
                "Fenetre aluminium 12 500 6000",
                "Cable principal 25",
                "Article libre sans mot cle"
        ));

        DqeAnalysisResultDTO result = dqeAnalysisService.analyze(file);

        assertThat(result.lignesAnalysees()).isEqualTo(3);
        assertThat(result.lignesSansPrix()).isEqualTo(2);
        assertThat(result.lignesSansQuantite()).isEqualTo(1);
        assertThat(result.lignesNonClassees()).isEqualTo(1);
        assertThat(result.scoreGlobal()).isBetween(40d, 90d);
        assertThat(result.lignes()).hasSize(3);
        assertThat(result.lignes().get(0).classee()).isTrue();
        assertThat(result.lignes().get(0).valide()).isTrue();
        assertThat(result.lignes().get(1).alertes()).contains("Prix unitaire manquant");
        assertThat(result.lignes().get(2).alertes()).contains("Classification incertaine");
    }
}
