package com.sp2i.core.dqe;

import com.sp2i.dto.dqe.DqeLineAnalysisDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Service d'enrichissement metier BTP + sourcing Afrique.
 */
@Service
public class DqeEnrichmentService {

    private final ImportSimulationService importSimulationService;
    private final DecisionService decisionService;

    public DqeEnrichmentService(
            ImportSimulationService importSimulationService,
            DecisionService decisionService
    ) {
        this.importSimulationService = importSimulationService;
        this.decisionService = decisionService;
    }

    public List<DqeLineAnalysisDTO> enrich(List<DqeLineAnalysisDTO> lines) {
        lines.forEach(this::enrichLine);
        return lines;
    }

    private void enrichLine(DqeLineAnalysisDTO line) {
        Double localPrice = line.getPrixUnitaire();
        if (localPrice == null || localPrice <= 0d) {
            localPrice = estimateLocalPrice(line);
        }

        Double importFob = estimateImportFobPrice(line, localPrice);
        Double importRendered = importSimulationService.calculateRenderedImportCost(importFob);
        String risk = evaluateRisk(line);
        String supplier = suggestSupplier(line);
        String decision = decisionService.decide(localPrice, importRendered, risk);

        line.setPrixLocalEstime(round(localPrice));
        line.setPrixImportEstime(round(importFob));
        line.setPrixImportRendu(round(importRendered));
        line.setRisque(risk);
        line.setFournisseurSuggestion(supplier);
        line.setDecision(decision);
        line.setScoreConfiance(computeConfidence(line));
    }

    private Double estimateLocalPrice(DqeLineAnalysisDTO line) {
        double base = switch (line.getLot()) {
            case "Menuiserie" -> "Portes".equals(line.getFamille()) ? 420d : 650d;
            case "Electricite" -> "Tableaux".equals(line.getFamille()) ? 520d : 140d;
            case "Plomberie" -> "Sanitaires".equals(line.getFamille()) ? 280d : 95d;
            case "CVC" -> "Climatisation".equals(line.getFamille()) ? 760d : 420d;
            case "Finitions" -> "Peinture".equals(line.getFamille()) ? 18d : 40d;
            case "Gros oeuvre" -> 130d;
            default -> 120d;
        };

        String normalized = safe(line.getDesignation()).toLowerCase(Locale.ROOT);
        if (normalized.contains("alu") || normalized.contains("premium")) {
            base *= 1.15d;
        }
        if (normalized.contains("sur mesure") || normalized.contains("industriel")) {
            base *= 1.2d;
        }
        return base;
    }

    private Double estimateImportFobPrice(DqeLineAnalysisDTO line, Double localPrice) {
        double ratio = switch (line.getLot()) {
            case "Menuiserie" -> 0.62d;
            case "Electricite" -> 0.55d;
            case "Plomberie" -> 0.68d;
            case "CVC" -> 0.72d;
            case "Finitions" -> 0.78d;
            case "Gros oeuvre" -> 0.85d;
            default -> 0.82d;
        };
        return localPrice * ratio;
    }

    private String evaluateRisk(DqeLineAnalysisDTO line) {
        String normalized = safe(line.getDesignation()).toLowerCase(Locale.ROOT);

        if ("CVC".equals(line.getLot()) || normalized.contains("tableau") || normalized.contains("sur mesure")) {
            return "ELEVE";
        }
        if ("Menuiserie".equals(line.getLot()) || "Plomberie".equals(line.getLot())) {
            return "MOYEN";
        }
        return "FAIBLE";
    }

    private String suggestSupplier(DqeLineAnalysisDTO line) {
        return switch (line.getLot()) {
            case "Menuiserie" -> "Fabricant menuiserie aluminium";
            case "Electricite" -> "Grossiste electrique export";
            case "Plomberie" -> "Distributeur plomberie sanitaire";
            case "CVC" -> "Fournisseur HVAC / OEM";
            case "Finitions" -> "Fournisseur finitions chantier";
            case "Gros oeuvre" -> "Negociant materiaux structure";
            default -> "Fournisseur generaliste BTP";
        };
    }

    private Double computeConfidence(DqeLineAnalysisDTO line) {
        double score = line.getScoreQualite() == null ? 0d : line.getScoreQualite();
        if ("ELEVE".equals(line.getRisque())) {
            score -= 10d;
        }
        if (line.getErreurs() != null) {
            score -= line.getErreurs().size() * 3d;
        }
        return Math.max(15d, Math.min(100d, round(score)));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private double round(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
