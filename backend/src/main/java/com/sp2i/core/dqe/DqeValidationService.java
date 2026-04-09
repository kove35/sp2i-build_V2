package com.sp2i.core.dqe;

import com.sp2i.dto.dqe.DqeLineAnalysisDTO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service de validation metier simple.
 */
@Service
public class DqeValidationService {

    public List<DqeLineAnalysisDTO> validate(List<DqeLineAnalysisDTO> lines) {
        lines.forEach(this::validateLine);
        return lines;
    }

    private void validateLine(DqeLineAnalysisDTO line) {
        if (line.getQuantite() == null || line.getQuantite() <= 0d) {
            line.getErreurs().add("QUANTITE_MANQUANTE");
        }
        if (line.getPrixUnitaire() == null || line.getPrixUnitaire() <= 0d) {
            line.getErreurs().add("PRIX_MANQUANT");
        }
        if (isBlank(line.getLot()) || isBlank(line.getFamille()) || "DQE".equals(line.getLot()) || "Autres".equals(line.getFamille())) {
            line.getErreurs().add("NON_CLASSE");
        }
        if (isBlank(line.getUnite())) {
            line.getErreurs().add("UNITE_MANQUANTE");
        }
        if (isBlank(line.getBatiment()) || line.getBatiment().contains("A_VERIFIER")) {
            line.getErreurs().add("BATIMENT_NON_IDENTIFIE");
        }
        if (isBlank(line.getNiveau()) || line.getNiveau().contains("A_VERIFIER")) {
            line.getErreurs().add("NIVEAU_NON_IDENTIFIE");
        }
        if (isAmountIncoherent(line)) {
            line.getErreurs().add("MONTANT_INCOHERENT");
        }

        line.setValide(line.getErreurs().stream().noneMatch(error ->
                error.equals("PRIX_MANQUANT")
                        || error.equals("QUANTITE_MANQUANTE")
                        || error.equals("NON_CLASSE")
                        || error.equals("MONTANT_INCOHERENT")
        ));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Controle simple de coherence metier.
     *
     * Si quantite * prix unitaire est tres eloigne du total lu,
     * on marque la ligne comme incoherente. Cela aide a reperer
     * les chiffres colles ou dupliques pendant l'extraction PDF.
     */
    private boolean isAmountIncoherent(DqeLineAnalysisDTO line) {
        if (line.getQuantite() == null || line.getPrixUnitaire() == null || line.getTotal() == null) {
            return false;
        }

        double expectedTotal = line.getQuantite() * line.getPrixUnitaire();
        double actualTotal = line.getTotal();

        if (expectedTotal <= 0d || actualTotal <= 0d) {
            return false;
        }

        double delta = Math.abs(expectedTotal - actualTotal);
        double tolerance = Math.max(expectedTotal * 0.02d, 1d);
        return delta > tolerance;
    }
}
