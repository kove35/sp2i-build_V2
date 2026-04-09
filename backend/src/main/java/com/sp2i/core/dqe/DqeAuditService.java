package com.sp2i.core.dqe;

import com.sp2i.dto.dqe.DqeAuditBlockDTO;
import com.sp2i.dto.dqe.DqeLineAnalysisDTO;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service d'audit de coherence du DQE.
 *
 * Son role est simple :
 * - relire le texte brut extrait du PDF
 * - detecter des blocs metier (batiment + niveau)
 * - additionner les lignes de prix detectees
 * - comparer cette somme au sous-total affiche dans le document
 *
 * Ce service ne remplace pas le pipeline principal.
 * Il sert de filet de securite pedagogique pour voir rapidement
 * si l'extraction colle bien au document source.
 */
@Service
public class DqeAuditService {

    private static final String FRENCH_AMOUNT_PATTERN = "(?:\\d{1,3}(?: \\d{3})+|\\d+)(?:[.,]\\d+)?";
    private static final Pattern BUILDING_HEADING_PATTERN =
            Pattern.compile("^B(?:Â|Ã‚)?TIMENT\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern STANDALONE_TOTAL_PATTERN =
            Pattern.compile("^" + FRENCH_AMOUNT_PATTERN + "$");

    private final DqeSemanticHelper semanticHelper;

    public DqeAuditService(DqeSemanticHelper semanticHelper) {
        this.semanticHelper = semanticHelper;
    }

    public List<DqeAuditBlockDTO> audit(String extractedText, List<DqeLineAnalysisDTO> analyzedLines) {
        List<DqeAuditBlockDTO> blocks = new ArrayList<>();
        List<String> lines = normalizeLines(extractedText);

        String currentBatiment = "BATIMENT_A_VERIFIER";
        String currentNiveau = "NIVEAU_A_VERIFIER";

        for (String line : lines) {
            Matcher buildingMatcher = BUILDING_HEADING_PATTERN.matcher(line);
            if (buildingMatcher.matches()) {
                currentBatiment = semanticHelper.normalizeBatimentHeading(buildingMatcher.group(1));
                continue;
            }

            if (isLevelHeading(line)) {
                currentNiveau = semanticHelper.normalizeLevelHeading(line);
                continue;
            }

            Matcher standaloneTotalMatcher = STANDALONE_TOTAL_PATTERN.matcher(line);
            if (!standaloneTotalMatcher.matches()) {
                continue;
            }

            Double documentSubtotal = parseFrenchNumber(line);
            if (documentSubtotal == null) {
                continue;
            }

            String blockBatiment = currentBatiment;
            String blockNiveau = currentNiveau;

            double calculatedSubtotal = analyzedLines.stream()
                    .filter(dto -> sameValue(dto.getBatiment(), blockBatiment))
                    .filter(dto -> sameValue(dto.getNiveau(), blockNiveau))
                    .mapToDouble(dto -> dto.getTotal() == null ? 0d : dto.getTotal())
                    .sum();

            int detectedLines = (int) analyzedLines.stream()
                    .filter(dto -> sameValue(dto.getBatiment(), blockBatiment))
                    .filter(dto -> sameValue(dto.getNiveau(), blockNiveau))
                    .count();

            if (detectedLines == 0) {
                continue;
            }

            double roundedCalculated = round(calculatedSubtotal);
            double roundedDocument = round(documentSubtotal);
            double delta = round(roundedDocument - roundedCalculated);
            boolean coherent = Math.abs(delta) <= Math.max(roundedDocument * 0.02d, 1d);

            blocks.add(new DqeAuditBlockDTO(
                    blockBatiment,
                    blockNiveau,
                    roundedDocument,
                    roundedCalculated,
                    delta,
                    coherent,
                    detectedLines
            ));
        }

        return blocks;
    }

    private List<String> normalizeLines(String text) {
        List<String> lines = new ArrayList<>();
        for (String rawLine : text.split("\\R")) {
            String normalized = rawLine == null ? "" : rawLine.replace('\t', ' ').replaceAll("\\s+", " ").trim();
            if (!normalized.isBlank()) {
                lines.add(normalized);
            }
        }
        return lines;
    }

    private boolean isLevelHeading(String line) {
        String normalized = canonicalizeLabel(line);
        return normalized.contains("fondations")
                || normalized.contains("elevation rdc")
                || normalized.contains("elevation etage 1")
                || normalized.contains("elevation etage 2")
                || normalized.contains("terrasse")
                || normalized.contains("duplex 1")
                || normalized.contains("duplex 2");
    }

    private Double parseFrenchNumber(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return Double.parseDouble(rawValue.replace("\u00A0", "").replace(" ", "").replace(",", "."));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private double round(double value) {
        return Math.round(value * 100d) / 100d;
    }

    private boolean sameValue(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return canonicalizeLabel(left).equals(canonicalizeLabel(right));
    }

    private String canonicalizeLabel(String value) {
        String normalized = value == null ? "" : value;

        // Certains PDF/OCR degradent les caracteres accentues.
        // On ramene ici toutes les variantes vers une forme stable
        // pour comparer correctement les blocs metier.
        normalized = normalized
                .replace("Ã‚", "Â")
                .replace("Ã¢", "â")
                .replace("Ã©", "é")
                .replace("Ã¨", "è")
                .replace("Ãª", "ê")
                .replace("Ã", "à");

        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        // Dans certains extraits, le niveau apparait comme "C FONDATIONS".
        // La lettre de section n'est pas un vrai niveau metier, donc on la retire.
        if (normalized.matches("^[a-z] fondations$")) {
            return "fondations";
        }

        return normalized;
    }
}
