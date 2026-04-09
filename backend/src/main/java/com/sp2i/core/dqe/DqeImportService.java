package com.sp2i.core.dqe;

import com.sp2i.core.exception.BusinessException;
import com.sp2i.dto.dqe.DqeLineAnalysisDTO;
import com.sp2i.dto.dqe.DqeImportResultDTO;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ce fichier sert a importer un DQE a partir d'un PDF, d'une image,
 * d'un Excel, d'un CSV, d'un JSON ou d'un TXT.
 *
 * La logique suit plusieurs etapes :
 * 1. extraire du texte depuis le document
 * 2. analyser les lignes avec des regex
 * 3. reconnaitre un lot et une famille via des mots cles
 * 4. convertir chaque ligne en CapexItem
 * 5. retourner un bilan avec succes + erreurs
 *
 * Le service est volontairement tres commente pour montrer
 * une facon simple de structurer un mini pipeline d'import.
 */
@Service
public class DqeImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DqeImportService.class);
    private static final DataFormatter DATA_FORMATTER = new DataFormatter(Locale.FRANCE);

    private final DqeService dqeService;
    private final DqeAiService dqeAiService;
    private final DqeSemanticHelper dqeSemanticHelper;

    /**
     * Chemin optionnel vers les donnees Tesseract.
     *
     * Si cette valeur n'est pas renseignee, Tess4J tentera
     * d'utiliser la configuration par defaut de la machine.
     */
    private final String tesseractDataPath;

    public DqeImportService(
            DqeService dqeService,
            DqeAiService dqeAiService,
            DqeSemanticHelper dqeSemanticHelper,
            @Value("${tesseract.datapath:}") String tesseractDataPath
    ) {
        this.dqeService = dqeService;
        this.dqeAiService = dqeAiService;
        this.dqeSemanticHelper = dqeSemanticHelper;
        this.tesseractDataPath = tesseractDataPath;
    }

    /**
     * Point d'entree demande par le besoin.
     *
     * Il lit seulement le document et renvoie des lignes DQE parsees.
     * La sauvegarde est faite dans une methode separée pour garder
     * le traitement plus clair.
     */
    public List<DqeService.ParsedDqeLine> parseDqeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Le fichier DQE est vide");
        }

        String extractedText = extractTextContent(file);

        if (extractedText.isBlank()) {
            throw new BusinessException("Aucun texte exploitable n'a ete trouve dans le document");
        }

        return parseExtractedText(extractedText);
    }

    /**
     * Importe un document DQE dans un projet.
     */
    public DqeImportResultDTO importDqeFile(Long projectId, MultipartFile file) {
        if (projectId == null) {
            throw new BusinessException("Le projectId est obligatoire");
        }

        List<DqeService.ParsedDqeLine> parsedLines = parseDqeFile(file);
        List<String> errors = new ArrayList<>();
        int importedLines = 0;

        for (DqeService.ParsedDqeLine parsedLine : parsedLines) {
            try {
                dqeService.createItemFromParsedLine(projectId, parsedLine);
                importedLines++;
            } catch (BusinessException exception) {
                errors.add(exception.getMessage() + " | ligne=" + parsedLine.toDebugMap());
            }
        }

        LOGGER.info("Import DQE PDF/Image termine : {} ligne(s) importee(s), {} erreur(s)", importedLines, errors.size());
        return new DqeImportResultDTO(importedLines, List.copyOf(errors));
    }

    /**
     * Extrait le texte brut d'un document DQE.
     *
     * Cette methode est volontairement reutilisable :
     * - l'import s'en sert pour sauver des lignes
     * - l'analyse s'en sert pour diagnostiquer un document sans import
     */
    String extractTextContent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Le fichier DQE est vide");
        }

        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        return extractText(file, fileName);
    }

    /**
     * Decoupe un texte extrait en lignes nettoyees.
     *
     * On renvoie seulement des lignes candidates utiles pour l'analyse.
     */
    List<String> normalizeDocumentLines(String extractedText) {
        List<String> normalizedLines = new ArrayList<>();

        for (String rawLine : extractedText.split("\\R")) {
            String line = normalizeLine(rawLine);
            if (!shouldIgnoreLine(line)) {
                normalizedLines.add(line);
            }
        }

        return normalizedLines;
    }

    private String extractText(MultipartFile file, String fileName) {
        if (fileName.endsWith(".pdf")) {
            return extractTextFromPdf(file);
        }

        if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".webp")) {
            return extractTextFromImage(file);
        }

        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return extractTextFromExcel(file);
        }

        if (fileName.endsWith(".csv")) {
            return extractTextFromCsv(file);
        }

        if (fileName.endsWith(".json")) {
            return extractTextFromJson(file);
        }

        if (fileName.endsWith(".txt")) {
            return extractTextFromTxt(file);
        }

        throw new BusinessException("Format non supporte. Utilise PDF, image, Excel, CSV, JSON ou TXT");
    }

    private String extractTextFromPdf(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper textStripper = new PDFTextStripper();
            String text = textStripper.getText(document);

            if (text != null && !text.isBlank()) {
                LOGGER.info("Extraction PDF texte terminee : {} caractere(s) lus", text.length());
                return text;
            }

            LOGGER.info("PDF sans texte exploitable detecte, bascule vers OCR des pages PDF");
            return extractTextFromPdfWithOcr(file);
        } catch (Exception exception) {
            LOGGER.warn("Lecture PDF texte impossible, tentative OCR PDF", exception);
            return extractTextFromPdfWithOcr(file);
        }
    }

    private String extractTextFromImage(MultipartFile file) {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("sp2i-dqe-", getSafeExtension(file.getOriginalFilename()));
            file.transferTo(tempFile);

            Tesseract tesseract = buildTesseract();

            String text = tesseract.doOCR(tempFile.toFile());
            LOGGER.info("Extraction OCR terminee : {} caractere(s) lus", text.length());
            return text;
        } catch (TesseractException exception) {
            throw new BusinessException("OCR impossible. Verifie l'installation de Tesseract sur la machine");
        } catch (IOException exception) {
            throw new BusinessException("Impossible de lire l'image DQE");
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    LOGGER.debug("Suppression temporaire impossible pour {}", tempFile);
                }
            }
        }
    }

    /**
     * Fallback pour les PDF scannes.
     *
     * On rend chaque page en image puis on applique l'OCR.
     * C'est plus lent qu'une lecture texte native, mais bien plus robuste
     * pour une demo reelle avec des documents chantier.
     */
    private String extractTextFromPdfWithOcr(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFRenderer renderer = new PDFRenderer(document);
            Tesseract tesseract = buildTesseract();
            StringBuilder builder = new StringBuilder();

            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                var renderedPage = renderer.renderImageWithDPI(pageIndex, 200);
                String pageText = tesseract.doOCR(renderedPage);
                if (pageText != null && !pageText.isBlank()) {
                    builder.append(pageText).append(System.lineSeparator());
                }
            }

            String text = builder.toString();
            if (text.isBlank()) {
                throw new BusinessException("Aucun texte exploitable n'a ete trouve dans le PDF");
            }

            LOGGER.info("Extraction OCR PDF terminee : {} caractere(s) lus", text.length());
            return text;
        } catch (TesseractException exception) {
            throw new BusinessException("OCR PDF impossible. Verifie l'installation de Tesseract sur la machine");
        } catch (IOException exception) {
            throw new BusinessException("Impossible de lire le fichier PDF");
        }
    }

    /**
     * Lit un fichier Excel comme une suite de lignes texte.
     *
     * Cette option est utile pour la demo produit :
     * certains DQE reels existent d'abord en tableur.
     * On convertit alors chaque ligne en texte simple pour reutiliser
     * le meme pipeline d'analyse que pour un PDF.
     */
    private String extractTextFromExcel(MultipartFile file) {
        StringBuilder builder = new StringBuilder();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet firstSheet = workbook.getSheetAt(0);

            for (Row row : firstSheet) {
                if (row.getRowNum() == 0) {
                    continue;
                }

                List<String> values = new ArrayList<>();

                for (int cellIndex = 0; cellIndex <= 6; cellIndex++) {
                    Cell cell = row.getCell(cellIndex);
                    String cellValue = cell == null ? "" : DATA_FORMATTER.formatCellValue(cell).trim();
                    if (!cellValue.isBlank()) {
                        values.add(cellValue);
                    }
                }

                if (!values.isEmpty()) {
                    builder.append(String.join(" ", values)).append(System.lineSeparator());
                }
            }
        } catch (IOException exception) {
            throw new BusinessException("Impossible de lire le fichier Excel DQE");
        }

        String extractedText = builder.toString();
        LOGGER.info("Extraction Excel terminee : {} caractere(s) lus", extractedText.length());
        return extractedText;
    }

    /**
     * Lit un fichier CSV comme une suite de lignes texte.
     *
     * Le but n'est pas de faire un parseur CSV complet ici,
     * mais de remettre le contenu dans un format simple pour
     * le pipeline DQE.
     */
    private String extractTextFromCsv(MultipartFile file) {
        try {
            String extractedText = new String(file.getBytes(), StandardCharsets.UTF_8);
            LOGGER.info("Extraction CSV terminee : {} caractere(s) lus", extractedText.length());
            return extractedText;
        } catch (IOException exception) {
            throw new BusinessException("Impossible de lire le fichier CSV DQE");
        }
    }

    /**
     * Lit un fichier JSON comme un texte brut.
     *
     * Cette approche est volontairement simple :
     * on laisse ensuite le moteur d'analyse reconnaitre
     * les designations, montants et contextes utiles.
     */
    private String extractTextFromJson(MultipartFile file) {
        try {
            String extractedText = new String(file.getBytes(), StandardCharsets.UTF_8);
            LOGGER.info("Extraction JSON terminee : {} caractere(s) lus", extractedText.length());
            return extractedText;
        } catch (IOException exception) {
            throw new BusinessException("Impossible de lire le fichier JSON DQE");
        }
    }

    /**
     * Lit un fichier texte simple.
     *
     * C'est le format le plus direct :
     * le contenu est deja proche du texte standardise attendu
     * par le moteur DQE.
     */
    private String extractTextFromTxt(MultipartFile file) {
        try {
            String extractedText = new String(file.getBytes(), StandardCharsets.UTF_8);
            LOGGER.info("Extraction TXT terminee : {} caractere(s) lus", extractedText.length());
            return extractedText;
        } catch (IOException exception) {
            throw new BusinessException("Impossible de lire le fichier TXT DQE");
        }
    }

    private List<DqeService.ParsedDqeLine> parseExtractedText(String extractedText) {
        List<DqeService.ParsedDqeLine> lines = new ArrayList<>();
        List<DqeLineAnalysisDTO> analyzedLines = dqeAiService.analyze(extractedText);

        for (DqeLineAnalysisDTO analyzedLine : analyzedLines) {
            if (analyzedLine.getDesignation() == null || analyzedLine.getDesignation().isBlank()) {
                continue;
            }
            if (analyzedLine.getQuantite() == null || analyzedLine.getPrixUnitaire() == null) {
                continue;
            }

            lines.add(new DqeService.ParsedDqeLine(
                    analyzedLine.getLot() == null ? dqeSemanticHelper.inferLot(analyzedLine.getDesignation()) : analyzedLine.getLot(),
                    analyzedLine.getFamille() == null
                            ? dqeSemanticHelper.inferFamille(analyzedLine.getDesignation(), analyzedLine.getLot())
                            : analyzedLine.getFamille(),
                    dqeSemanticHelper.sanitize(analyzedLine.getDesignation()),
                    analyzedLine.getUnite() == null ? dqeSemanticHelper.inferUnit(analyzedLine.getDesignation()) : analyzedLine.getUnite(),
                    analyzedLine.getBatiment(),
                    analyzedLine.getNiveau(),
                    analyzedLine.getQuantite(),
                    analyzedLine.getPrixUnitaire(),
                    analyzedLine.getTotal()
            ));
        }

        if (lines.isEmpty()) {
            throw new BusinessException("Aucune ligne DQE exploitable n'a ete detectee");
        }

        LOGGER.info("Parsing DQE termine : {} ligne(s) reconnue(s)", lines.size());
        return lines;
    }

    private String normalizeLine(String rawLine) {
        return rawLine == null ? "" : rawLine.replace('\t', ' ').replaceAll("\\s+", " ").trim();
    }

    private boolean shouldIgnoreLine(String line) {
        if (line.isBlank()) {
            return true;
        }

        String lowerCaseLine = line.toLowerCase(Locale.ROOT);
        return lowerCaseLine.contains("designation")
                || lowerCaseLine.contains("quantite")
                || lowerCaseLine.contains("prix unitaire")
                || lowerCaseLine.contains("montant");
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

    private String getSafeExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".tmp";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }

    private Tesseract buildTesseract() {
        Tesseract tesseract = new Tesseract();
        if (!tesseractDataPath.isBlank()) {
            tesseract.setDatapath(tesseractDataPath);
        }
        return tesseract;
    }
}
