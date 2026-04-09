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

    public DqeAiAnalysisService(DqeImportService dqeImportService) {
        this.dqeImportService = dqeImportService;
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
        String lot = inferLot(designation);
        String famille = inferFamille(designation, lot);
        String unite = inferUnit(designation);

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

        if ("DQE".equals(lot) || "Autres".equals(famille)) {
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
            case "Menuiserie" -> famille.equals("Portes") ? 420d : 650d;
            case "Electricite" -> famille.equals("Tableaux") ? 520d : 140d;
            case "Plomberie" -> famille.equals("Sanitaires") ? 280d : 95d;
            case "CVC" -> famille.equals("Climatisation") ? 750d : 410d;
            case "Finitions" -> famille.equals("Peinture") ? 18d : 40d;
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
            case "Menuiserie" -> famille.equals("Fenetres") ? 0.62d : 0.7d;
            case "Electricite" -> 0.55d;
            case "Plomberie" -> 0.68d;
            case "CVC" -> 0.72d;
            case "Finitions" -> 0.78d;
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
            case "Menuiserie" -> "Fabricant menuiserie aluminium";
            case "Electricite" -> famille.equals("Tableaux")
                    ? "Integrateur electrique industriel"
                    : "Grossiste electrique export";
            case "Plomberie" -> "Distributeur plomberie sanitaire";
            case "CVC" -> "Fabricant HVAC / OEM";
            case "Finitions" -> "Fournisseur finition chantier";
            default -> "Fournisseur generaliste BTP";
        };
    }

    private String evaluateImportRisk(String lot, String famille, String designation) {
        String normalized = designation.toLowerCase(Locale.ROOT);

        if ("CVC".equals(lot) || normalized.contains("sur mesure") || normalized.contains("tableau")) {
            return "ELEVE";
        }
        if ("Menuiserie".equals(lot) || "Plomberie".equals(lot) || famille.equals("Sanitaires")) {
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

        if ("DQE".equals(lot) || "Autres".equals(famille)) {
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

    private String inferLot(String designation) {
        String normalized = designation.toLowerCase(Locale.ROOT);

        if (normalized.contains("fenetre") || normalized.contains("porte") || normalized.contains("vitrage")) {
            return "Menuiserie";
        }
        if (normalized.contains("cable") || normalized.contains("eclairage") || normalized.contains("tableau")) {
            return "Electricite";
        }
        if (normalized.contains("tuyau") || normalized.contains("robinet") || normalized.contains("sanitaire")) {
            return "Plomberie";
        }
        if (normalized.contains("clim") || normalized.contains("ventilation") || normalized.contains("split")) {
            return "CVC";
        }
        if (normalized.contains("peinture") || normalized.contains("enduit") || normalized.contains("faux plafond")) {
            return "Finitions";
        }
        return "DQE";
    }

    private String inferFamille(String designation, String lot) {
        String normalized = designation.toLowerCase(Locale.ROOT);

        return switch (lot) {
            case "Menuiserie" -> normalized.contains("porte") ? "Portes" : "Fenetres";
            case "Electricite" -> normalized.contains("tableau") ? "Tableaux" : "Cables";
            case "Plomberie" -> normalized.contains("sanitaire") ? "Sanitaires" : "Tuyauterie";
            case "CVC" -> normalized.contains("ventilation") ? "Ventilation" : "Climatisation";
            case "Finitions" -> normalized.contains("faux plafond") ? "Faux plafond" : "Peinture";
            default -> "Autres";
        };
    }

    private String inferUnit(String designation) {
        String normalized = designation.toLowerCase(Locale.ROOT);
        if (normalized.contains("m2")) {
            return "m2";
        }
        if (normalized.contains("ml")) {
            return "ml";
        }
        return "U";
    }

    private double round(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
