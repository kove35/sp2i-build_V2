package com.sp2i.core.dqe;

import com.sp2i.core.exception.BusinessException;
import com.sp2i.dto.dqe.DqeUploadResultDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

/**
 * Service dedie a l'etape d'upload et d'extraction standardisee.
 */
@Service
public class DocumentExtractionService {

    private final DqeImportService dqeImportService;

    public DocumentExtractionService(DqeImportService dqeImportService) {
        this.dqeImportService = dqeImportService;
    }

    public DqeUploadResultDTO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Le fichier DQE est vide");
        }

        String fileName = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        String documentType = detectType(fileName);
        String extractedText = dqeImportService.extractTextContent(file);
        return new DqeUploadResultDTO(fileName, documentType, extractedText);
    }

    public String extract(MultipartFile file) {
        return upload(file).extractedText();
    }

    private String detectType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return "PDF";
        }
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp")) {
            return "IMAGE";
        }
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
            return "EXCEL";
        }
        if (lower.endsWith(".csv")) {
            return "CSV";
        }
        if (lower.endsWith(".json")) {
            return "JSON";
        }
        if (lower.endsWith(".txt")) {
            return "TXT";
        }
        throw new BusinessException("Format non supporte. Utilise PDF, image, Excel, CSV, JSON ou TXT");
    }
}
