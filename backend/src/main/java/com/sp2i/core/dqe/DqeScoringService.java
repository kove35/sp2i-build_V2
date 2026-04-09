package com.sp2i.core.dqe;

import com.sp2i.dto.dqe.DqeLineAnalysisDTO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service de scoring qualite des lignes DQE.
 */
@Service
public class DqeScoringService {

    public List<DqeLineAnalysisDTO> scoreLines(List<DqeLineAnalysisDTO> lines) {
        lines.forEach(this::scoreLine);
        return lines;
    }

    public double computeGlobalScore(List<DqeLineAnalysisDTO> lines) {
        return lines.stream()
                .mapToDouble(line -> line.getScoreQualite() == null ? 0d : line.getScoreQualite())
                .average()
                .orElse(0d);
    }

    private void scoreLine(DqeLineAnalysisDTO line) {
        double score = 0d;

        if (line.getPrixUnitaire() != null && line.getPrixUnitaire() > 0d) {
            score += 40d;
        }
        if (line.getQuantite() != null && line.getQuantite() > 0d) {
            score += 30d;
        }
        if (line.getLot() != null && !line.getLot().isBlank() && !"DQE".equals(line.getLot())
                && line.getFamille() != null && !line.getFamille().isBlank() && !"Autres".equals(line.getFamille())) {
            score += 20d;
        }
        if (line.getUnite() != null && !line.getUnite().isBlank()) {
            score += 10d;
        }

        line.setScoreQualite(score);
    }
}
