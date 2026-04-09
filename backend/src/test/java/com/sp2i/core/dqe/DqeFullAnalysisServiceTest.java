package com.sp2i.core.dqe;

import com.sp2i.dto.dqe.DqeFullAnalysisResultDTO;
import com.sp2i.dto.dqe.DqeLineAnalysisDTO;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DqeFullAnalysisServiceTest {

    private final DocumentExtractionService documentExtractionService = mock(DocumentExtractionService.class);
    private final DqeAiService dqeAiService = mock(DqeAiService.class);
    private final DqeAuditService dqeAuditService = new DqeAuditService(new DqeSemanticHelper());
    private final DqeValidationService dqeValidationService = new DqeValidationService();
    private final DqeScoringService dqeScoringService = new DqeScoringService();
    private final DqeEnrichmentService dqeEnrichmentService = mock(DqeEnrichmentService.class);

    private final DqeFullAnalysisService dqeFullAnalysisService = new DqeFullAnalysisService(
            documentExtractionService,
            dqeAiService,
            dqeAuditService,
            dqeValidationService,
            dqeScoringService,
            dqeEnrichmentService
    );

    @Test
    void shouldBuildGlobalKpisFromAnalyzedLines() {
        MultipartFile file = mock(MultipartFile.class);

        DqeLineAnalysisDTO line = new DqeLineAnalysisDTO();
        line.setDesignation("Fenetre aluminium");
        line.setLot("Menuiserie");
        line.setFamille("Fenetres");
        line.setBatiment("Batiment A");
        line.setNiveau("R+1");
        line.setQuantite(10d);
        line.setUnite("U");
        line.setPrixUnitaire(500d);
        line.setPrixLocalEstime(500d);
        line.setPrixImportRendu(350d);

        when(documentExtractionService.extract(file)).thenReturn("Fenetre");
        when(dqeAiService.analyze("Fenetre")).thenReturn(List.of(line));
        when(dqeEnrichmentService.enrich(List.of(line))).thenReturn(List.of(line));

        DqeFullAnalysisResultDTO result = dqeFullAnalysisService.analyzeFull(file);

        assertThat(result.lignesValides()).isEqualTo(1);
        assertThat(result.lignesErreur()).isEqualTo(0);
        assertThat(result.scoreGlobal()).isEqualTo(100d);
        assertThat(result.lignesSansBatiment()).isEqualTo(0);
        assertThat(result.lignesSansNiveau()).isEqualTo(0);
        assertThat(result.capexTotal()).isEqualTo(5000d);
        assertThat(result.capexOptimise()).isEqualTo(3500d);
        assertThat(result.economie()).isEqualTo(1500d);
        assertThat(result.auditBlocs()).isEmpty();
    }
}
