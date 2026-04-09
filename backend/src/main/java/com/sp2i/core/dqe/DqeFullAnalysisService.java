package com.sp2i.core.dqe;

import com.sp2i.dto.dqe.DqeFullAnalysisResultDTO;
import com.sp2i.dto.dqe.DqeLineAnalysisDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrateur du pipeline intelligent complet.
 */
@Service
public class DqeFullAnalysisService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DqeFullAnalysisService.class);

    private final DocumentExtractionService documentExtractionService;
    private final DqeAiService dqeAiService;
    private final DqeAuditService dqeAuditService;
    private final DqeValidationService dqeValidationService;
    private final DqeScoringService dqeScoringService;
    private final DqeEnrichmentService dqeEnrichmentService;

    public DqeFullAnalysisService(
            DocumentExtractionService documentExtractionService,
            DqeAiService dqeAiService,
            DqeAuditService dqeAuditService,
            DqeValidationService dqeValidationService,
            DqeScoringService dqeScoringService,
            DqeEnrichmentService dqeEnrichmentService
    ) {
        this.documentExtractionService = documentExtractionService;
        this.dqeAiService = dqeAiService;
        this.dqeAuditService = dqeAuditService;
        this.dqeValidationService = dqeValidationService;
        this.dqeScoringService = dqeScoringService;
        this.dqeEnrichmentService = dqeEnrichmentService;
    }

    public DqeFullAnalysisResultDTO analyzeFull(MultipartFile file) {
        String extractedText = documentExtractionService.extract(file);
        List<DqeLineAnalysisDTO> lines = dqeAiService.analyze(extractedText);
        dqeValidationService.validate(lines);
        dqeScoringService.scoreLines(lines);
        dqeEnrichmentService.enrich(lines);
        var auditBlocs = dqeAuditService.audit(extractedText, lines);

        double scoreGlobal = round(dqeScoringService.computeGlobalScore(lines));
        int lignesValides = (int) lines.stream().filter(DqeLineAnalysisDTO::isValide).count();
        int lignesErreur = lines.size() - lignesValides;
        int lignesSansPrix = (int) lines.stream().filter(line -> hasError(line, "PRIX_MANQUANT")).count();
        int lignesSansQuantite = (int) lines.stream().filter(line -> hasError(line, "QUANTITE_MANQUANTE")).count();
        int lignesNonClassees = (int) lines.stream().filter(line -> hasError(line, "NON_CLASSE")).count();
        int lignesSansBatiment = (int) lines.stream().filter(line -> hasError(line, "BATIMENT_NON_IDENTIFIE")).count();
        int lignesSansNiveau = (int) lines.stream().filter(line -> hasError(line, "NIVEAU_NON_IDENTIFIE")).count();

        double capexTotal = lines.stream()
                .mapToDouble(line -> safe(line.getPrixLocalEstime()) * safe(line.getQuantite()))
                .sum();
        double capexOptimise = lines.stream()
                .mapToDouble(line -> Math.min(safe(line.getPrixLocalEstime()), safe(line.getPrixImportRendu())) * safe(line.getQuantite()))
                .sum();
        double economie = capexTotal - capexOptimise;

        List<String> alertes = new ArrayList<>();
        if (lignesSansPrix > 0) {
            alertes.add(lignesSansPrix + " ligne(s) sans prix fiable");
        }
        if (lignesSansQuantite > 0) {
            alertes.add(lignesSansQuantite + " ligne(s) sans quantite fiable");
        }
        if (lignesNonClassees > 0) {
            alertes.add(lignesNonClassees + " ligne(s) non classees");
        }
        if (lignesSansBatiment > 0) {
            alertes.add(lignesSansBatiment + " ligne(s) sans batiment fiable");
        }
        if (lignesSansNiveau > 0) {
            alertes.add(lignesSansNiveau + " ligne(s) sans niveau fiable");
        }
        if (scoreGlobal < 70d) {
            alertes.add("Le document doit etre relu avant import complet");
        }
        long auditIncoherentCount = auditBlocs.stream().filter(block -> !block.coherent()).count();
        if (auditIncoherentCount > 0) {
            alertes.add(auditIncoherentCount + " bloc(s) DQE ont un sous-total incoherent");
        }

        LOGGER.info(
                "DQE full analysis : {} ligne(s), scoreGlobal={}, capexTotal={}, capexOptimise={}",
                lines.size(),
                scoreGlobal,
                capexTotal,
                capexOptimise
        );

        return new DqeFullAnalysisResultDTO(
                scoreGlobal,
                lignesValides,
                lignesErreur,
                lignesSansPrix,
                lignesSansQuantite,
                lignesNonClassees,
                lignesSansBatiment,
                lignesSansNiveau,
                round(capexTotal),
                round(capexOptimise),
                round(economie),
                List.copyOf(alertes),
                auditBlocs,
                lines
        );
    }

    private boolean hasError(DqeLineAnalysisDTO line, String code) {
        return line.getErreurs() != null && line.getErreurs().contains(code);
    }

    private double safe(Double value) {
        return value == null ? 0d : value;
    }

    private double round(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
