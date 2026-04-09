package com.sp2i.core.dqe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp2i.core.openai.OpenAIService;
import com.sp2i.dto.dqe.DqeLineAnalysisDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service d'analyse IA du texte DQE.
 *
 * Si OpenAI n'est pas configure, on bascule en fallback heuristique
 * pour que l'application reste utilisable en local.
 */
@Service
public class DqeAiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DqeAiService.class);
    private static final String FRENCH_AMOUNT_PATTERN = "(?:\\d{1,3}(?: \\d{3})+|\\d+)(?:[.,]\\d+)?";
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(?:[.,]\\d+)?");
    private static final Pattern LOT_HEADING_PATTERN = Pattern.compile("^LOT\\s+\\d+\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BUILDING_HEADING_PATTERN = Pattern.compile("^B(?:ATIMENT|ÂTIMENT|Ã‚TIMENT)?\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ITEM_LINE_PATTERN = Pattern.compile(
            "^(?:\\d+\\s+)?(.+?)\\s+(Ens|FF|Forfait|FORFAIT|m2|m3|ml|m|kg|u)\\s+(\\d+(?:[.,]\\d+)?)\\s+(" + FRENCH_AMOUNT_PATTERN + ")\\s+(PM|" + FRENCH_AMOUNT_PATTERN + ")$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ITEM_WITHOUT_DESIGNATION_PATTERN = Pattern.compile(
            "^(?:\\d+\\s+)?(Ens|FF|Forfait|FORFAIT|m2|m3|ml|m|kg|u)\\s+(\\d+(?:[.,]\\d+)?)\\s+(" + FRENCH_AMOUNT_PATTERN + ")\\s+(PM|" + FRENCH_AMOUNT_PATTERN + ")$",
            Pattern.CASE_INSENSITIVE
    );

    private final ObjectMapper objectMapper;
    private final DqeSemanticHelper semanticHelper;
    private final OpenAIService openAIService;

    public DqeAiService(
            ObjectMapper objectMapper,
            DqeSemanticHelper semanticHelper,
            OpenAIService openAIService
    ) {
        this.objectMapper = objectMapper;
        this.semanticHelper = semanticHelper;
        this.openAIService = openAIService;
    }

    public List<DqeLineAnalysisDTO> analyze(String text) {
        List<DqeLineAnalysisDTO> csvStructuredLines = parseReferenceCsvDocument(text);
        if (!csvStructuredLines.isEmpty()) {
            LOGGER.info("Analyse DQE directe depuis CSV structure : {} ligne(s)", csvStructuredLines.size());
            return csvStructuredLines;
        }

        List<DqeLineAnalysisDTO> jsonStructuredLines = parseReferenceJsonDocument(text);
        if (!jsonStructuredLines.isEmpty()) {
            LOGGER.info("Analyse DQE directe depuis JSON structure : {} ligne(s)", jsonStructuredLines.size());
            return jsonStructuredLines;
        }

        try {
            return analyzeWithOpenAi(text);
        } catch (Exception exception) {
            LOGGER.warn("Analyse OpenAI impossible, fallback heuristique actif", exception);
            return fallbackAnalyze(text);
        }
    }

    private List<DqeLineAnalysisDTO> analyzeWithOpenAi(String text) throws Exception {
        String prompt = """
                Tu es un expert en DQE batiment.
                Transforme ce texte en lignes structurees.
                Retourne uniquement un JSON tableau valide.
                Champs :
                designation, quantite, unite, prixUnitaire, total, lot, famille, batiment, niveau, erreurs.
                Regles :
                - corriger incoherences
                - calculer total si manquant
                - classifier intelligemment
                - si batiment ou niveau sont introuvables, utiliser BATIMENT_A_VERIFIER et NIVEAU_A_VERIFIER

                TEXTE A ANALYSER :
                """ + System.lineSeparator() + text;

        String content = openAIService.callOpenAI(prompt);
        return objectMapper.readValue(
                content,
                objectMapper.getTypeFactory().constructCollectionType(List.class, DqeLineAnalysisDTO.class)
        );
    }

    private List<DqeLineAnalysisDTO> fallbackAnalyze(String text) {
        List<DqeLineAnalysisDTO> structuredLines = parseStructuredDqeDocument(text);
        if (!structuredLines.isEmpty()) {
            return structuredLines;
        }

        List<DqeLineAnalysisDTO> lines = new ArrayList<>();

        for (String rawLine : text.split("\\R")) {
            String normalized = semanticHelper.sanitize(rawLine == null ? "" : rawLine.replace('\t', ' '))
                    .replaceAll("\\s+", " ")
                    .trim();
            if (normalized.isBlank() || shouldIgnoreLine(normalized)) {
                continue;
            }

            DqeLineAnalysisDTO line = new DqeLineAnalysisDTO();
            List<Double> numbers = extractNumbers(normalized);
            String designation = extractDesignation(normalized);
            String lot = semanticHelper.inferLot(designation);
            String famille = semanticHelper.inferFamille(designation, lot);

            line.setDesignation(designation);
            line.setQuantite(numbers.size() >= 1 ? numbers.get(0) : null);
            line.setPrixUnitaire(numbers.size() >= 2 ? numbers.get(1) : null);
            line.setTotal(numbers.size() >= 3 ? numbers.get(2) : deriveTotal(line.getQuantite(), line.getPrixUnitaire()));
            line.setLot(lot);
            line.setFamille(famille);
            line.setUnite(semanticHelper.inferUnit(designation));
            line.setBatiment(semanticHelper.inferBatiment(normalized));
            line.setNiveau(semanticHelper.inferNiveau(normalized));

            if (!designation.isBlank()) {
                lines.add(line);
            }
        }

        return lines;
    }

    /**
     * Lit directement le format tabulaire de reference :
     * Lot;Sous-lot;Bâtiment;Niveau;Désignation;Unité;Quantité;PU;Total
     *
     * Ce format est tres utile quand un audit humain a deja remis
     * les donnees en ordre dans Excel, CSV ou TXT.
     */
    private List<DqeLineAnalysisDTO> parseReferenceCsvDocument(String text) {
        String trimmedText = text == null ? "" : text.trim();
        if (trimmedText.isBlank()) {
            return List.of();
        }

        List<String> rows = new ArrayList<>();
        for (String rawLine : trimmedText.split("\\R")) {
            String normalizedLine = rawLine == null ? "" : rawLine.trim();
            if (!normalizedLine.isBlank()) {
                rows.add(normalizedLine);
            }
        }

        if (rows.isEmpty()) {
            return List.of();
        }

        String rawHeader = rows.get(0);
        String delimiter = detectStructuredTableDelimiter(rawHeader);
        if (delimiter == null) {
            return List.of();
        }

        String asciiHeader = stripAccents(rawHeader)
                .toLowerCase(Locale.ROOT)
                .replace("\uFEFF", "")
                .replace('\t', ';');
        if (!asciiHeader.contains("lot;") || !asciiHeader.contains("designation") || !asciiHeader.contains("total")) {
            return List.of();
        }

        List<DqeLineAnalysisDTO> structuredLines = new ArrayList<>();

        for (int index = 1; index < rows.size(); index++) {
            String[] columns = rows.get(index).split(Pattern.quote(delimiter), -1);
            if (columns.length < 9) {
                continue;
            }

            String lotNumber = semanticHelper.sanitize(columns[0]);
            String sousLot = semanticHelper.sanitize(columns[1]);
            String batiment = semanticHelper.sanitize(columns[2]);
            String niveau = semanticHelper.sanitize(columns[3]);
            String designation = semanticHelper.sanitize(columns[4]);
            String unite = semanticHelper.sanitize(columns[5]);
            Double quantite = parseFrenchNumber(columns[6]);
            Double prixUnitaire = parseFrenchNumber(columns[7]);
            Double total = parseFrenchNumber(columns[8]);

            if (designation.isBlank()) {
                continue;
            }

            String normalizedLot = normalizeLotFromCsv(lotNumber, sousLot, designation);
            DqeLineAnalysisDTO line = new DqeLineAnalysisDTO();
            line.setDesignation(designation);
            line.setUnite(unite.isBlank() ? semanticHelper.inferUnit(designation) : unite);
            line.setQuantite(quantite);
            line.setPrixUnitaire(prixUnitaire);
            line.setTotal(total != null ? total : deriveTotal(quantite, prixUnitaire));
            line.setLot(normalizedLot);
            line.setFamille(semanticHelper.inferFamille(designation, normalizedLot));
            line.setBatiment(normalizeCsvBatiment(batiment));
            line.setNiveau(normalizeCsvNiveau(niveau));
            structuredLines.add(line);
        }

        return structuredLines;
    }

    private String detectStructuredTableDelimiter(String headerLine) {
        if (headerLine == null || headerLine.isBlank()) {
            return null;
        }

        if (headerLine.contains(";")) {
            return ";";
        }
        if (headerLine.contains("\t")) {
            return "\t";
        }
        return null;
    }

    /**
     * Lit directement le format JSON structure fourni comme reference metier.
     *
     * Ce chemin est important :
     * - il evite de retransformer un JSON propre en texte brut
     * - il preserve mieux lot / section / designation / montants
     * - il sert de format premium pour l'import et l'analyse
     */
    private List<DqeLineAnalysisDTO> parseReferenceJsonDocument(String text) {
        String trimmedText = text == null ? "" : text.trim();
        if (trimmedText.isBlank() || !trimmedText.startsWith("{")) {
            return List.of();
        }

        try {
            JsonNode rootNode = objectMapper.readTree(trimmedText);
            if (rootNode == null || !rootNode.has("lots") || !rootNode.get("lots").isArray()) {
                return List.of();
            }

            List<DqeLineAnalysisDTO> structuredLines = new ArrayList<>();

            for (JsonNode lotNode : rootNode.path("lots")) {
                String lotDescription = semanticHelper.sanitize(lotNode.path("description").asText(""));
                String normalizedLot = semanticHelper.normalizeLotHeading(lotDescription);

                for (JsonNode sectionNode : lotNode.path("sections")) {
                    String sectionName = semanticHelper.sanitize(sectionNode.path("nom").asText(""));
                    String sectionBatiment = semanticHelper.inferBatiment(sectionName);
                    String sectionNiveau = semanticHelper.inferNiveau(sectionName);

                    for (JsonNode detailNode : sectionNode.path("details")) {
                        String designation = semanticHelper.sanitize(detailNode.path("designation").asText(""));
                        String unite = semanticHelper.sanitize(detailNode.path("unite").asText(""));
                        Double quantite = readNumber(detailNode.get("quantite"));
                        Double prixUnitaire = readNumber(detailNode.get("prix_unitaire"));
                        Double total = readNumber(detailNode.get("total"));

                        if (designation.isBlank()) {
                            continue;
                        }

                        DqeLineAnalysisDTO line = new DqeLineAnalysisDTO();
                        line.setDesignation(designation);
                        line.setUnite(unite.isBlank() ? semanticHelper.inferUnit(designation) : unite);
                        line.setQuantite(quantite);
                        line.setPrixUnitaire(prixUnitaire);
                        line.setTotal(total != null ? total : deriveTotal(quantite, prixUnitaire));
                        line.setLot(normalizedLot);
                        line.setFamille(semanticHelper.inferFamille(designation, normalizedLot));
                        line.setBatiment(resolveJsonBatiment(designation, sectionBatiment));
                        line.setNiveau(resolveJsonLevel(designation, sectionName, sectionNiveau));
                        repairCommonPdfAmountArtifacts(line);
                        structuredLines.add(line);
                    }
                }
            }

            return structuredLines;
        } catch (Exception exception) {
            LOGGER.debug("Le contenu n'est pas un JSON DQE structure reconnu", exception);
            return List.of();
        }
    }

    /**
     * Parseur specialise pour les PDF DQE chantier.
     *
     * Le document reel a une structure repetitive :
     * - LOT x : ...
     * - BATIMENT ...
     * - section / niveau
     * - lignes d'articles avec unite, quantite, prix unitaire, total
     *
     * Cette methode exploite cette structure pour produire des lignes
     * plus propres que le fallback texte libre.
     */
    private List<DqeLineAnalysisDTO> parseStructuredDqeDocument(String text) {
        List<DqeLineAnalysisDTO> lines = new ArrayList<>();
        List<String> normalizedLines = normalizeLines(text);

        String currentLot = "";
        String currentBatiment = "Site";
        String currentNiveau = "GLOBAL";
        String currentSection = "";
        String pendingDesignation = "";

        for (int index = 0; index < normalizedLines.size(); index++) {
            String line = semanticHelper.sanitize(normalizedLines.get(index));

            Matcher lotMatcher = LOT_HEADING_PATTERN.matcher(line);
            if (lotMatcher.matches()) {
                currentLot = semanticHelper.normalizeLotHeading(lotMatcher.group(1));
                currentSection = "";
                pendingDesignation = "";
                continue;
            }

            Matcher buildingMatcher = BUILDING_HEADING_PATTERN.matcher(line);
            if (buildingMatcher.matches()) {
                currentBatiment = semanticHelper.normalizeBatimentHeading(buildingMatcher.group(1));
                continue;
            }

            if (looksLikeCombinedBuildingAndLevelHeading(line)) {
                currentBatiment = semanticHelper.inferBatiment(line);
                currentNiveau = semanticHelper.inferNiveau(line);
                currentSection = "";
                pendingDesignation = "";
                continue;
            }

            if (isLevelHeading(line)) {
                currentNiveau = semanticHelper.normalizeLevelHeading(line);
                currentSection = "";
                pendingDesignation = "";
                continue;
            }

            if (isSectionTitle(line)) {
                currentSection = stripSectionPrefix(line);
                if (isGlobalSection(currentSection)) {
                    currentBatiment = "Site";
                    currentNiveau = "GLOBAL";
                }
                pendingDesignation = currentSection;
                continue;
            }

            if (shouldIgnoreLine(line) || isSubtotalLine(line) || isDocumentNoise(line)) {
                continue;
            }

            Matcher noDesignationMatcher = ITEM_WITHOUT_DESIGNATION_PATTERN.matcher(line);
            if (noDesignationMatcher.matches()) {
                String designation = pendingDesignation;
                if (index + 1 < normalizedLines.size() && isFreeTextContinuation(normalizedLines.get(index + 1))) {
                    designation = mergeDesignation(designation, normalizedLines.get(index + 1));
                    index++;
                }
                designation = applySectionContext(designation, currentSection);

                DqeLineAnalysisDTO dto = createStructuredLine(
                        designation,
                        noDesignationMatcher.group(1),
                        noDesignationMatcher.group(2),
                        noDesignationMatcher.group(3),
                        noDesignationMatcher.group(4),
                        currentLot,
                        currentBatiment,
                        resolveLevel(designation, currentNiveau)
                );
                if (dto != null) {
                    lines.add(dto);
                }
                pendingDesignation = "";
                continue;
            }

            Matcher itemMatcher = ITEM_LINE_PATTERN.matcher(line);
            if (itemMatcher.matches()) {
                String designation = itemMatcher.group(1);
                if (designation.matches("^\\d+$")) {
                    designation = pendingDesignation;
                }
                if (!pendingDesignation.isBlank() && isShortDesignation(designation)) {
                    designation = mergeDesignation(pendingDesignation, designation);
                }
                if (index + 1 < normalizedLines.size() && isFreeTextContinuation(normalizedLines.get(index + 1))) {
                    designation = mergeDesignation(designation, normalizedLines.get(index + 1));
                    index++;
                }
                designation = applySectionContext(designation, currentSection);

                DqeLineAnalysisDTO dto = createStructuredLine(
                        designation,
                        itemMatcher.group(2),
                        itemMatcher.group(3),
                        itemMatcher.group(4),
                        itemMatcher.group(5),
                        currentLot,
                        currentBatiment,
                        resolveLevel(designation, currentNiveau)
                );
                if (dto != null) {
                    lines.add(dto);
                }
                pendingDesignation = "";
                continue;
            }

            if (isFreeTextContinuation(line)) {
                pendingDesignation = mergeDesignation(pendingDesignation, line);
            }
        }

        return lines;
    }

    private List<String> normalizeLines(String text) {
        List<String> lines = new ArrayList<>();
        for (String rawLine : text.split("\\R")) {
            String normalized = semanticHelper.sanitize(rawLine == null ? "" : rawLine.replace('\t', ' '))
                    .replaceAll("\\s+", " ")
                    .trim();
            if (!normalized.isBlank()) {
                lines.add(normalized);
            }
        }
        return lines;
    }

    private DqeLineAnalysisDTO createStructuredLine(
            String designation,
            String unite,
            String quantiteValue,
            String prixValue,
            String totalValue,
            String lot,
            String batiment,
            String niveau
    ) {
        if (designation == null || designation.isBlank()) {
            return null;
        }

        DqeLineAnalysisDTO line = new DqeLineAnalysisDTO();
        line.setDesignation(cleanDesignation(designation));
        line.setUnite(unite == null ? semanticHelper.inferUnit(designation) : semanticHelper.sanitize(unite));
        line.setQuantite(parseFrenchNumber(quantiteValue));
        line.setPrixUnitaire(parseFrenchNumber(prixValue));

        Double total = "PM".equalsIgnoreCase(safe(totalValue))
                ? deriveTotal(line.getQuantite(), line.getPrixUnitaire())
                : parseFrenchNumber(totalValue);
        line.setTotal(total);
        repairCommonPdfAmountArtifacts(line);
        line.setLot(lot == null || lot.isBlank() ? semanticHelper.inferLot(designation) : lot);
        line.setFamille(semanticHelper.inferFamille(line.getDesignation(), line.getLot()));
        line.setBatiment(resolveBatiment(designation, batiment));
        line.setNiveau(resolveLevel(designation, niveau));
        return line;
    }

    private boolean isLevelHeading(String line) {
        String normalized = semanticHelper.sanitize(line).toLowerCase(Locale.ROOT);
        return normalized.contains("fondations")
                || normalized.contains("rez de chaussee")
                || normalized.contains("elevation rdc")
                || normalized.contains("elevation etage 1")
                || normalized.contains("elevation etage 2")
                || normalized.contains("terrasse")
                || normalized.contains("duplex 1")
                || normalized.contains("duplex 2");
    }

    private boolean isSectionTitle(String line) {
        return line.matches("^\\d+\\s+.+$")
                && !ITEM_LINE_PATTERN.matcher(line).matches()
                && !ITEM_WITHOUT_DESIGNATION_PATTERN.matcher(line).matches();
    }

    private String stripSectionPrefix(String line) {
        return semanticHelper.sanitize(line.replaceFirst("^\\d+\\s+", "").trim());
    }

    private boolean isSubtotalLine(String line) {
        String lower = semanticHelper.sanitize(line).toLowerCase(Locale.ROOT);
        return lower.startsWith("sous total") || lower.startsWith("total ht");
    }

    private boolean isDocumentNoise(String line) {
        String lower = semanticHelper.sanitize(line).toLowerCase(Locale.ROOT);
        return lower.contains("construct devis")
                || lower.contains("medical4center")
                || lower.contains("medical7center")
                || lower.contains("entreprise de construction")
                || lower.contains("pointe-noire")
                || lower.contains("socoprise")
                || lower.startsWith("nº ")
                || lower.startsWith("n° ")
                || lower.startsWith("projet :")
                || lower.startsWith("recapitulatif");
    }

    private boolean isFreeTextContinuation(String line) {
        if (line.isBlank()) {
            return false;
        }
        if (isDocumentNoise(line) || isSubtotalLine(line) || isLevelHeading(line)) {
            return false;
        }
        if (LOT_HEADING_PATTERN.matcher(line).matches() || BUILDING_HEADING_PATTERN.matcher(line).matches()) {
            return false;
        }
        return !ITEM_LINE_PATTERN.matcher(line).matches()
                && !ITEM_WITHOUT_DESIGNATION_PATTERN.matcher(line).matches();
    }

    private boolean isShortDesignation(String designation) {
        return designation != null && designation.trim().split("\\s+").length <= 2;
    }

    private String mergeDesignation(String left, String right) {
        String mergedLeft = safe(left);
        String mergedRight = safe(right);
        if (mergedLeft.isBlank()) {
            return semanticHelper.sanitize(mergedRight);
        }
        if (mergedRight.isBlank()) {
            return semanticHelper.sanitize(mergedLeft);
        }
        return semanticHelper.sanitize(mergedLeft + " - " + mergedRight);
    }

    private String applySectionContext(String designation, String currentSection) {
        String cleanDesignation = semanticHelper.sanitize(designation);
        String cleanSection = semanticHelper.sanitize(currentSection);

        if (cleanSection.isBlank()) {
            return cleanDesignation;
        }
        if (cleanDesignation.isBlank()) {
            return cleanSection;
        }
        if (cleanDesignation.toLowerCase(Locale.ROOT).contains(cleanSection.toLowerCase(Locale.ROOT))) {
            return cleanDesignation;
        }
        return mergeDesignation(cleanSection, cleanDesignation);
    }

    private boolean isGlobalSection(String sectionTitle) {
        String normalizedSection = semanticHelper.sanitize(sectionTitle).toLowerCase(Locale.ROOT);
        return normalizedSection.contains("installation generale")
                || normalizedSection.contains("chantier")
                || normalizedSection.contains("mobilisation")
                || normalizedSection.contains("etudes techniques")
                || normalizedSection.contains("essais");
    }

    private String resolveBatiment(String designation, String currentBatiment) {
        String inferredBatiment = semanticHelper.inferBatiment(designation);
        if (inferredBatiment != null && !inferredBatiment.contains("A_VERIFIER")) {
            return inferredBatiment;
        }
        return currentBatiment;
    }

    private String resolveLevel(String designation, String currentNiveau) {
        String inferredLevel = semanticHelper.inferNiveau(designation);
        if (inferredLevel != null && !inferredLevel.contains("A_VERIFIER")) {
            return inferredLevel;
        }
        return currentNiveau;
    }

    private String cleanDesignation(String designation) {
        return semanticHelper.sanitize(safe(designation))
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean looksLikeCombinedBuildingAndLevelHeading(String line) {
        String sanitized = semanticHelper.sanitize(line);
        return sanitized.toLowerCase(Locale.ROOT).contains("batiment")
                && !semanticHelper.inferBatiment(sanitized).contains("A_VERIFIER")
                && !semanticHelper.inferNiveau(sanitized).contains("A_VERIFIER");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean shouldIgnoreLine(String line) {
        String lower = semanticHelper.sanitize(line).toLowerCase(Locale.ROOT);
        return lower.contains("designation")
                || lower.contains("quantite")
                || lower.contains("prix unitaire")
                || lower.contains("montant");
    }

    private List<Double> extractNumbers(String line) {
        List<Double> numbers = new ArrayList<>();
        Matcher matcher = NUMBER_PATTERN.matcher(line);

        while (matcher.find()) {
            Double value = parseFrenchNumber(matcher.group());
            if (value != null) {
                numbers.add(value);
            }
        }

        return numbers;
    }

    private String extractDesignation(String line) {
        Matcher matcher = NUMBER_PATTERN.matcher(line);
        if (matcher.find()) {
            return semanticHelper.sanitize(line.substring(0, matcher.start()).trim());
        }
        return semanticHelper.sanitize(line.trim());
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

    private Double readNumber(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        return parseFrenchNumber(node.asText());
    }

    private String resolveJsonBatiment(String designation, String sectionBatiment) {
        String inferredBatiment = semanticHelper.inferBatiment(designation);
        if (!inferredBatiment.contains("A_VERIFIER")) {
            return inferredBatiment;
        }
        return sectionBatiment;
    }

    private String resolveJsonLevel(String designation, String sectionName, String sectionNiveau) {
        String inferredLevel = semanticHelper.inferNiveau(designation);
        if (!inferredLevel.contains("A_VERIFIER")) {
            return inferredLevel;
        }

        if (!sectionNiveau.contains("A_VERIFIER")) {
            return sectionNiveau;
        }

        String normalizedSection = semanticHelper.sanitize(sectionName).toLowerCase(Locale.ROOT);
        if (normalizedSection.contains("principal") || normalizedSection.contains("annexe")) {
            return "GLOBAL";
        }
        return sectionNiveau;
    }

    private String normalizeLotFromCsv(String lotNumber, String sousLot, String designation) {
        String normalizedSousLot = semanticHelper.sanitize(sousLot).toLowerCase(Locale.ROOT);

        if ("1".equals(lotNumber)) {
            return "Gros oeuvre";
        }
        if ("2".equals(lotNumber)) {
            return "Etancheite";
        }
        if ("3".equals(lotNumber) || "11".equals(lotNumber) || "13".equals(lotNumber) || "14".equals(lotNumber)) {
            return "Finitions";
        }
        if ("4".equals(lotNumber) || "5".equals(lotNumber) || "6".equals(lotNumber)) {
            return "Menuiserie";
        }
        if ("7".equals(lotNumber) || "9".equals(lotNumber)) {
            return "Electricite";
        }
        if ("8".equals(lotNumber)) {
            return "CVC";
        }
        if ("10".equals(lotNumber)) {
            return normalizedSousLot.contains("ventilation") ? "CVC" : "Plomberie";
        }
        if ("12".equals(lotNumber)) {
            return "Equipement";
        }

        if (!sousLot.isBlank()) {
            return semanticHelper.normalizeLotHeading(sousLot);
        }
        return semanticHelper.inferLot(designation);
    }

    private String normalizeCsvBatiment(String batiment) {
        String normalizedBatiment = semanticHelper.sanitize(batiment).toLowerCase(Locale.ROOT);
        if (normalizedBatiment.contains("chantier")) {
            return "Site";
        }
        return semanticHelper.normalizeBatimentHeading(batiment);
    }

    private String normalizeCsvNiveau(String niveau) {
        String normalizedNiveau = semanticHelper.sanitize(niveau).toLowerCase(Locale.ROOT);
        if (normalizedNiveau.contains("niveau chantier")) {
            return "GLOBAL";
        }
        if (normalizedNiveau.contains("parties communes chantier")) {
            return "PARTIES COMMUNES CHANTIER";
        }
        if (normalizedNiveau.contains("parties communes")) {
            return "PARTIES COMMUNES";
        }
        return semanticHelper.normalizeLevelHeading(niveau);
    }

    private Double deriveTotal(Double quantity, Double price) {
        if (quantity == null || price == null) {
            return null;
        }
        return quantity * price;
    }

    private String stripAccents(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    /**
     * Corrige quelques artefacts typiques de lecture PDF.
     *
     * Quand la quantite vaut 1, le total doit normalement etre
     * egal au prix unitaire. Si le total lu est aberrant, on
     * recolle le comportement metier attendu.
     */
    private void repairCommonPdfAmountArtifacts(DqeLineAnalysisDTO line) {
        if (line.getQuantite() == null || line.getPrixUnitaire() == null) {
            return;
        }

        if (line.getTotal() == null && Math.abs(line.getQuantite() - 1d) < 0.0001d) {
            line.setTotal(line.getPrixUnitaire());
            return;
        }

        if (line.getTotal() == null || line.getTotal() <= 0d) {
            return;
        }

        double expectedTotal = line.getQuantite() * line.getPrixUnitaire();
        if (expectedTotal <= 0d) {
            return;
        }

        double ratio = Math.max(expectedTotal, line.getTotal()) / Math.min(expectedTotal, line.getTotal());
        if (ratio > 100d && Math.abs(line.getQuantite() - 1d) < 0.0001d) {
            line.setTotal(line.getPrixUnitaire());
        }
    }
}
