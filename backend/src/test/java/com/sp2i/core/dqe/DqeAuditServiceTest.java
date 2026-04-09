package com.sp2i.core.dqe;

import com.sp2i.dto.dqe.DqeAuditBlockDTO;
import com.sp2i.dto.dqe.DqeLineAnalysisDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DqeAuditServiceTest {

    private final DqeAuditService dqeAuditService = new DqeAuditService(new DqeSemanticHelper());

    @Test
    void shouldAuditFoundationsBlockAgainstDocumentSubtotal() {
        String extractedText = """
                C FONDATIONS
                BÂTIMENT PRINCIPAL
                1 Semelles isolées
                Béton de propreté m3 3.07 338 720 1 039 870
                Béton 400 hydr m3 4.36 189 175 824 803
                Coffrage m2 15.84 5 750 91 080
                Acier kg 436 1 380 601 680
                2 Amorces de poteaux
                Béton 400 hydr m3 1.65 189 175 312 139
                Coffrage m2 26.4 5 750 151 800
                Acier kg 198 1 380 273 240
                3 Longrines
                Béton 400 hydr m3 3.72 189 175 703 731
                Coffrage m2 37.33 5 750 214 648
                Acier kg 484 1 380 667 920
                4 Dallage sol
                Béton m3 33.2 189 175 6 280 610
                Coffrage m2 66.8 5 750 384 100
                Acier kg 2324 1 380 3 207 120
                5 Maçonnerie de soubassement
                Agglos pleins 20x20x50 m2 78.98 12 180 961 976
                6 Dallage sol
                Béton m3 30.3 189 175 5 732 003
                Coffrage m2 10.8 5 750 62 100
                Acier kg 1818 1 380 2 508 840
                24 017 660
                """;

        List<DqeLineAnalysisDTO> analyzedLines = List.of(
                createLine("Batiment principal", "Fondations", 1_039_870d),
                createLine("Batiment principal", "Fondations", 824_803d),
                createLine("Batiment principal", "Fondations", 91_080d),
                createLine("Batiment principal", "Fondations", 601_680d),
                createLine("Batiment principal", "Fondations", 312_139d),
                createLine("Batiment principal", "Fondations", 151_800d),
                createLine("Batiment principal", "Fondations", 273_240d),
                createLine("Batiment principal", "Fondations", 703_731d),
                createLine("Batiment principal", "Fondations", 214_648d),
                createLine("Batiment principal", "Fondations", 667_920d),
                createLine("Batiment principal", "Fondations", 6_280_610d),
                createLine("Batiment principal", "Fondations", 384_100d),
                createLine("Batiment principal", "Fondations", 3_207_120d),
                createLine("Batiment principal", "Fondations", 961_976d),
                createLine("Batiment principal", "Fondations", 5_732_003d),
                createLine("Batiment principal", "Fondations", 62_100d),
                createLine("Batiment principal", "Fondations", 2_508_840d)
        );

        List<DqeAuditBlockDTO> result = dqeAuditService.audit(extractedText, analyzedLines);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).batiment()).containsIgnoringCase("principal");
        assertThat(result.get(0).niveau()).containsIgnoringCase("fondations");
        assertThat(result.get(0).sousTotalDocument()).isEqualTo(24_017_660d);
        assertThat(result.get(0).sousTotalCalcule()).isEqualTo(24_017_660d);
        assertThat(result.get(0).ecart()).isEqualTo(0d);
        assertThat(result.get(0).coherent()).isTrue();
        assertThat(result.get(0).lignesDetectees()).isEqualTo(17);
    }

    private DqeLineAnalysisDTO createLine(String batiment, String niveau, Double total) {
        DqeLineAnalysisDTO line = new DqeLineAnalysisDTO();
        line.setBatiment(batiment);
        line.setNiveau(niveau);
        line.setTotal(total);
        return line;
    }
}
