package com.sp2i.core.dqe;

import com.sp2i.infrastructure.persistence.ImportParameterRepository;
import org.springframework.stereotype.Service;

/**
 * Calcule le cout import complet rendu Pointe-Noire.
 */
@Service
public class ImportSimulationService {

    private final ImportParameterRepository importParameterRepository;

    public ImportSimulationService(ImportParameterRepository importParameterRepository) {
        this.importParameterRepository = importParameterRepository;
    }

    public double calculateRenderedImportCost(double fobPrice) {
        return fobPrice
                * (1 + getRate("TRANSPORT"))
                * (1 + getRate("ASSURANCE"))
                * (1 + getRate("DOUANE"))
                * (1 + getRate("PORT"))
                * (1 + getRate("LOCAL"))
                * (1 + getRate("RISQUE"));
    }

    private double getRate(String code) {
        return importParameterRepository.findByCode(code)
                .map(parameter -> parameter.getRate() == null ? 0d : parameter.getRate().doubleValue())
                .orElse(0d);
    }
}
