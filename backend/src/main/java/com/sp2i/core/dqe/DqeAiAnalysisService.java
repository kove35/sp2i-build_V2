package com.sp2i.core.dqe;

import com.sp2i.core.exception.BusinessException;
import com.sp2i.dto.dqe.DqeAiAnalysisResultDTO;
import com.sp2i.dto.dqe.DqeAiLineDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ce service realise une analyse "intelligente" du DQE
 * sans importer les donnees en base.
 *
 * L'objectif de la demo est de montrer qu'on peut :
 * - lire un document brut
 * - comprendre des lignes chantier
 * - estimer les prix manquants
 * - classifier lot / famille
 * - suggerer une strategie local vs import
 *
 * Ici, on reste sur des heuristiques pedagogiques.
 * Elles sont simples a lire pour un debutant et faciles
 * a remplacer plus tard par un moteur plus avance.
 */
@Service
public class DqeAiAnalysisService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DqeAiAnalysisService.class);
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(?:[.,]\\d+)?");

    private final DqeImportService dqeImportService;
    private final DqeSemanticHelper semanticHelper;

    public DqeAiAnalysisService(DqeImportService dqeImportService, DqeSemanticHelper semanticHelper) {
        this.dqeImportService = dqeImportService;
        this.semanticHelper = semanticHelper;
    }

    /**
     * Point d'entree principal de l'analyse IA.
     *
     * Pipeline :
     * 1. extraction texte
     * 2. parsing lignes
     * 3. normalisation
     * 4. classification
     * 5. enrichissement prix / fournisseur / risque
     * 6. scoring global
     */
    public DqeAiAnalysisResultDTO analyze(MultipartFile file) {
        String extractedText = dqeImportService.extractTextContent(file);
        List<String> rawLines = dqeImportService.normalizeDocumentLines(extractedText);

        if (rawLines.isEmpty()) {
            throw new BusinessException("Aucune ligne exploitable n'a ete detectee dans le document");
        }

        List<DqeAiLineDTO> lines = rawLines.stream()
                .map(this::analyzeLine)
                .filter(line -> line.designation() != null && !line.designation().isBlank())
                .toList();

        if (lines.isEmpty()) {
            throw new BusinessException("Aucune ligne exploitable n'a ete detectee dans le document");
        }

        double scoreGlobal = lines.stream()
                .mapToDouble(line -> line.scoreConfiance() == null ? 0d : line.scoreConfiance())
                .average()
                .orElse(0d);

        int lignesAvecAlerte = (int) lines.stream()
                .filter(line -> line.alertes() != null && !line.alertes().isEmpty())
                .count();

        int lignesSansPrix = (int) lines.stream()
                .filter(line -> containsAlert(line, "Prix local estime"))
                .count();

        int lignesSansQuantite = (int) lines.stream()
                .filter(line -> containsAlert(line, "Quantite estimee"))
                .count();

        int lignesNonClassees = (int) lines.stream()
                .filter(line -> containsAlert(line, "Classification incertaine"))
                .count();

        List<String> erreurs = new ArrayList<>();
        if (lignesSansPrix > 0) {
            erreurs.add(lignesSansPrix + " ligne(s) sans prix local fiable");
        }
        if (lignesSansQuantite > 0) {
            erreurs.add(lignesSansQuantite + " ligne(s) avec quantite estimee");
        }
        if (lignesNonClassees > 0) {
            erreurs.add(lignesNonClassees + " ligne(s) avec classification incertaine");
        }
        if (scoreGlobal < 60d) {
            erreurs.add("Le score global est faible : une relecture metier est recommandee");
        }

        LOGGER.info(
                "Analyse AI DQE terminee : {} ligne(s), scoreGlobal={}, alertes={}",
                lines.size(),
                round(scoreGlobal),
                lignesAvecAlerte
        );

        return new DqeAiAnalysisResultDTO(
                round(scoreGlobal),
                lines.size(),
                lignesAvecAlerte,
                List.copyOf(erreurs),
                lines
        );
    }

    private DqeAiLineDTO analyzeLine(String rawLine) {
        List<Double> numbers = extractNumbers(rawLine);
        List<String> alertes = new ArrayList<>();

        String designation = extractDesignation(rawLine);
        String lot = semanticHelper.inferLot(designation);
        String famille = semanticHelper.inferFamille(designation, lot);
        String unite = semanticHelper.inferUnit(designation);

        Double quantite = numbers.size() >= 1 ? numbers.get(0) : 1d;
        if (numbers.isEmpty()) {
            alertes.add("Quantite estimee");
        }

        Double prixLocal = numbers.size() >= 2 ? numbers.get(1) : estimateLocalPrice(designation, lot, famille);
        if (numbers.size() < 2) {
            alertes.add("Prix local estime");
        }

        Double prixImport = estimateImportFobPrice(prixLocal, lot, famille, designation);
        String fournisseurSuggestion = suggestSupplierType(lot, famille);
        String niveauRisque = evaluateImportRisk(lot, famille, designation);
        String decision = suggestDecision(prixLocal, prixImport, niveauRisque);

        if (DqeSemanticHelper.LOT_INCONNU.equals(lot) || "Autres".equals(famille)) {
            alertes.add("Classification incertaine");
        }
        if ("ELEVE".equals(niveauRisque)) {
            alertes.add("Risque import eleve");
        }

        double scoreConfiance = computeConfidenceScore(lot, famille, numbers, niveauRisque, alertes);

        return new DqeAiLineDTO(
                designation,
                round(quantite),
                unite,
                round(prixLocal),
                round(prixImport),
                lot,
                famille,
                fournisseurSuggestion,
                niveauRisque,
                decision,
                round(scoreConfiance),
                List.copyOf(alertes)
        );
    }

    private List<Double> extractNumbers(String line) {
        List<Double> values = new ArrayList<>();
        Matcher matcher = NUMBER_PATTERN.matcher(line);

        while (matcher.find()) {
            Double parsed = parseFrenchNumber(matcher.group());
            if (parsed != null) {
                values.add(parsed);
            }
        }

        return values;
    }

    private String extractDesignation(String line) {
        Matcher matcher = NUMBER_PATTERN.matcher(line);
        if (matcher.find()) {
            return line.substring(0, matcher.start()).trim();
        }
        return line.trim();
    }

    private Double estimateLocalPrice(String designation, String lot, String famille) {
        double base = switch (lot) {
            case DqeSemanticHelper.LOT_MENUISERIE_ALU -> famille.equals("Portes aluminium") ? 420d : 650d;
            case DqeSemanticHelper.LOT_MENUISERIE_METALLIQUE -> 460d;
            case DqeSemanticHelper.LOT_MENUISERIE_BOIS -> 380d;
            case DqeSemanticHelper.LOT_ELECTRICITE -> famille.equals("Tableaux electriques") ? 520d : 140d;
            case DqeSemanticHelper.LOT_PLOMBERIE -> famille.equals("Appareils sanitaires") ? 280d : 95d;
            case DqeSemanticHelper.LOT_CLIMATISATION -> famille.equals("Climatisation / splits") ? 750d : 410d;
            case DqeSemanticHelper.LOT_PEINTURE, DqeSemanticHelper.LOT_REVETEMENTS_DURS, DqeSemanticHelper.LOT_FAUX_PLAFOND -> 40d;
            default -> 120d;
        };

        String normalized = designation.toLowerCase(Locale.ROOT);
        if (normalized.contains("premium") || normalized.contains("alu")) {
            base *= 1.15d;
        }
        if (normalized.contains("lourd") || normalized.contains("industriel")) {
            base *= 1.25d;
        }

        return base;
    }

    private Double estimateImportFobPrice(Double prixLocal, String lot, String famille, String designation) {
        double ratio = switch (lot) {
            case DqeSemanticHelper.LOT_MENUISERIE_ALU -> famille.equals("Facades vitrees") ? 0.62d : 0.7d;
            case DqeSemanticHelper.LOT_MENUISERIE_METALLIQUE -> 0.74d;
            case DqeSemanticHelper.LOT_MENUISERIE_BOIS -> 0.76d;
            case DqeSemanticHelper.LOT_ELECTRICITE, DqeSemanticHelper.LOT_SECURITE -> 0.55d;
            case DqeSemanticHelper.LOT_PLOMBERIE -> 0.68d;
            case DqeSemanticHelper.LOT_CLIMATISATION -> 0.72d;
            case DqeSemanticHelper.LOT_PEINTURE, DqeSemanticHelper.LOT_REVETEMENTS_DURS, DqeSemanticHelper.LOT_FAUX_PLAFOND -> 0.78d;
            default -> 0.82d;
        };

        String normalized = designation.toLowerCase(Locale.ROOT);
        if (normalized.contains("sur mesure") || normalized.contains("specifique")) {
            ratio += 0.08d;
        }

        return prixLocal * ratio;
    }

    private String suggestSupplierType(String lot, String famille) {
        return switch (lot) {
            case DqeSemanticHelper.LOT_MENUISERIE_ALU -> "Fabricant menuiserie aluminium";
            case DqeSemanticHelper.LOT_MENUISERIE_METALLIQUE -> "Atelier serrurerie / ferronnerie";
            case DqeSemanticHelper.LOT_MENUISERIE_BOIS -> "Menuisier bois / fabricant local";
            case DqeSemanticHelper.LOT_ELECTRICITE -> famille.equals("Tableaux electriques")
                    ? "Integrateur electrique industriel"
                    : "Grossiste electrique export";
            case DqeSemanticHelper.LOT_SECURITE -> "Integrateur securite incendie et surete";
            case DqeSemanticHelper.LOT_PLOMBERIE -> "Distributeur plomberie sanitaire";
            case DqeSemanticHelper.LOT_CLIMATISATION -> "Fabricant HVAC / OEM";
            case DqeSemanticHelper.LOT_PEINTURE,
                 DqeSemanticHelper.LOT_REVETEMENTS_DURS,
                 DqeSemanticHelper.LOT_FAUX_PLAFOND -> "Fournisseur finition chantier";
            default -> "Fournisseur generaliste BTP";
        };
    }

    private String evaluateImportRisk(String lot, String famille, String designation) {
        String normalized = designation.toLowerCase(Locale.ROOT);

        if (DqeSemanticHelper.LOT_CLIMATISATION.equals(lot) || normalized.contains("sur mesure") || normalized.contains("tableau")) {
            return "ELEVE";
        }
        if (lot.startsWith("Menuiserie") || DqeSemanticHelper.LOT_PLOMBERIE.equals(lot) || famille.equals("Appareils sanitaires")) {
            return "MOYEN";
        }
        return "FAIBLE";
    }

    private String suggestDecision(Double prixLocal, Double prixImport, String niveauRisque) {
        if (prixLocal == null || prixImport == null || prixLocal <= 0d) {
            return "MIX";
        }

        double gainRatio = (prixLocal - prixImport) / prixLocal;

        if (gainRatio >= 0.20d && !"ELEVE".equals(niveauRisque)) {
            return "IMPORT";
        }
        if (gainRatio <= 0.08d || "ELEVE".equals(niveauRisque)) {
            return "LOCAL";
        }
        return "MIX";
    }

    private double computeConfidenceScore(
            String lot,
            String famille,
            List<Double> numbers,
            String niveauRisque,
            List<String> alertes
    ) {
        double score = 100d;

        if (numbers.isEmpty()) {
            score -= 35d;
        } else if (numbers.size() == 1) {
            score -= 20d;
        }

        if (DqeSemanticHelper.LOT_INCONNU.equals(lot) || "Autres".equals(famille)) {
            score -= 20d;
        }
        if ("ELEVE".equals(niveauRisque)) {
            score -= 10d;
        }
        score -= alertes.size() * 5d;

        return Math.max(15d, score);
    }

    private boolean containsAlert(DqeAiLineDTO line, String token) {
        return line.alertes() != null && line.alertes().stream().anyMatch(alert -> alert.contains(token));
    }

    private Double parseFrenchNumber(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String normalized = rawValue
                .replace("\u00A0", "")
                .replace(" ", "")
                .replace(",", ".");

        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private double round(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
