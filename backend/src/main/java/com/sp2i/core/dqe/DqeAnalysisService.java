package com.sp2i.core.dqe;

import com.sp2i.core.exception.BusinessException;
import com.sp2i.dto.dqe.DqeAnalysisLineDTO;
import com.sp2i.dto.dqe.DqeAnalysisResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ce service analyse un DQE sans l'importer en base.
 *
 * L'idee metier est la suivante :
 * - lire le document
 * - comprendre chaque ligne probable
 * - normaliser les valeurs detectees
 * - classifier le lot et la famille
 * - verifier si la ligne est exploitable
 * - calculer un score de confiance
 *
 * On obtient ainsi un diagnostic complet avant toute insertion.
 */
@Service
public class DqeAnalysisService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DqeAnalysisService.class);

    /**
     * Cette regex detecte des nombres au format francais simple.
     *
     * Elle sert a retrouver :
     * - la quantite
     * - le prix unitaire
     * - le total
     */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(?:[.,]\\d+)?");

    private final DqeImportService dqeImportService;
    private final DqeSemanticHelper semanticHelper;

    public DqeAnalysisService(DqeImportService dqeImportService, DqeSemanticHelper semanticHelper) {
        this.dqeImportService = dqeImportService;
        this.semanticHelper = semanticHelper;
    }

    /**
     * Point d'entree principal de l'analyse.
     *
     * Pipeline :
     * 1. extraction texte
     * 2. parsing lignes
     * 3. normalisation
     * 4. classification
     * 5. validation
     * 6. scoring
     */
    public DqeAnalysisResultDTO analyze(MultipartFile file) {
        String extractedText = dqeImportService.extractTextContent(file);
        List<String> documentLines = dqeImportService.normalizeDocumentLines(extractedText);

        if (documentLines.isEmpty()) {
            throw new BusinessException("Aucune ligne DQE exploitable n'a ete detectee");
        }

        List<DqeAnalysisLineDTO> analyzedLines = documentLines.stream()
                .map(this::analyzeLine)
                .filter(line -> line.designation() != null && !line.designation().isBlank())
                .toList();

        if (analyzedLines.isEmpty()) {
            throw new BusinessException("Aucune ligne DQE exploitable n'a ete detectee");
        }

        int lignesSansPrix = (int) analyzedLines.stream().filter(line -> line.prixUnitaire() == null).count();
        int lignesSansQuantite = (int) analyzedLines.stream().filter(line -> line.quantite() == null).count();
        int lignesNonClassees = (int) analyzedLines.stream().filter(line -> !line.classee()).count();

        double scoreGlobal = analyzedLines.stream()
                .mapToDouble(DqeAnalysisLineDTO::score)
                .average()
                .orElse(0d);

        LOGGER.info(
                "Analyse DQE terminee : {} ligne(s), scoreGlobal={}, sansPrix={}, sansQuantite={}, nonClassees={}",
                analyzedLines.size(),
                scoreGlobal,
                lignesSansPrix,
                lignesSansQuantite,
                lignesNonClassees
        );

        return new DqeAnalysisResultDTO(
                round(scoreGlobal),
                analyzedLines.size(),
                lignesSansPrix,
                lignesSansQuantite,
                lignesNonClassees,
                analyzedLines
        );
    }

    /**
     * Analyse une seule ligne brute issue du document.
     */
    private DqeAnalysisLineDTO analyzeLine(String line) {
        List<Double> detectedNumbers = extractNumbers(line);
        String designation = extractDesignation(line);
        String lot = semanticHelper.inferLot(designation);
        String famille = semanticHelper.inferFamille(designation, lot);

        Double quantite = detectedNumbers.size() >= 1 ? detectedNumbers.get(0) : null;
        Double prixUnitaire = detectedNumbers.size() >= 2 ? detectedNumbers.get(1) : null;
        Double prixTotal = detectedNumbers.size() >= 3 ? detectedNumbers.get(2) : deriveTotal(quantite, prixUnitaire);

        List<String> alertes = new ArrayList<>();

        if (quantite == null) {
            alertes.add("Quantite manquante");
        }
        if (prixUnitaire == null) {
            alertes.add("Prix unitaire manquant");
        }
        if (DqeSemanticHelper.LOT_INCONNU.equals(lot) || "Autres".equals(famille)) {
            alertes.add("Classification incertaine");
        }
        if (prixTotal == null) {
            alertes.add("Total non calcule");
        }

        boolean classee = !DqeSemanticHelper.LOT_INCONNU.equals(lot) && !"Autres".equals(famille);
        boolean valide = quantite != null && prixUnitaire != null;
        double score = computeScore(quantite, prixUnitaire, prixTotal, classee);

        return new DqeAnalysisLineDTO(
                designation,
                lot,
                famille,
                quantite,
                prixUnitaire,
                prixTotal,
                classee,
                valide,
                round(score),
                List.copyOf(alertes)
        );
    }

    /**
     * Recupere les nombres visibles dans une ligne.
     *
     * Exemple :
     * "Fenetre coulissante 12 500 6000"
     * donnera [12, 500, 6000]
     */
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

    /**
     * La designation est la partie texte situee avant le premier nombre detecte.
     */
    private String extractDesignation(String line) {
        Matcher matcher = NUMBER_PATTERN.matcher(line);
        if (matcher.find()) {
            return line.substring(0, matcher.start()).trim();
        }
        return line.trim();
    }

    private double computeScore(Double quantite, Double prixUnitaire, Double prixTotal, boolean classee) {
        double score = 100d;

        if (quantite == null) {
            score -= 35d;
        }
        if (prixUnitaire == null) {
            score -= 35d;
        }
        if (!classee) {
            score -= 20d;
        }
        if (prixTotal == null) {
            score -= 10d;
        }

        return Math.max(0d, score);
    }

    private Double deriveTotal(Double quantite, Double prixUnitaire) {
        if (quantite == null || prixUnitaire == null) {
            return null;
        }
        return quantite * prixUnitaire;
    }

    private Double parseFrenchNumber(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String normalizedValue = rawValue
                .replace("\u00A0", "")
                .replace(" ", "")
                .replace(",", ".");

        try {
            return Double.parseDouble(normalizedValue);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private double round(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
