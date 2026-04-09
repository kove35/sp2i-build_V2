package com.sp2i.core.capex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Ce composant sert a transformer un prix FOB Chine
 * en cout import reel rendu Pointe-Noire.
 *
 * Pourquoi separer ce calcul dans une classe dediee ?
 * - le calcul pourra etre reutilise ailleurs
 * - les taux restent configurables
 * - CapexService garde un role plus clair
 *
 * FOB signifie "Free On Board" :
 * c'est un prix de depart qui ne couvre pas encore
 * tout le cout logistique et commercial final.
 */
@Component
public class ImportCostCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportCostCalculator.class);

    private final double transportRate;
    private final double insuranceRate;
    private final double douaneRate;
    private final double portRate;
    private final double localTransportRate;
    private final double marginRate;

    public ImportCostCalculator(
            @Value("${import-cost.transport-rate:0.08}") double transportRate,
            @Value("${import-cost.insurance-rate:0.02}") double insuranceRate,
            @Value("${import-cost.douane-rate:0.15}") double douaneRate,
            @Value("${import-cost.port-rate:0.05}") double portRate,
            @Value("${import-cost.local-transport-rate:0.05}") double localTransportRate,
            @Value("${import-cost.margin-rate:0.10}") double marginRate
    ) {
        this.transportRate = transportRate;
        this.insuranceRate = insuranceRate;
        this.douaneRate = douaneRate;
        this.portRate = portRate;
        this.localTransportRate = localTransportRate;
        this.marginRate = marginRate;
    }

    /**
     * Calcule le prix import reel a partir d'un prix FOB.
     *
     * Formule :
     * puImport = puFob
     *         * (1 + transportRate)
     *         * (1 + insuranceRate)
     *         * (1 + douaneRate)
     *         * (1 + portRate)
     *         * (1 + localTransportRate)
     *         * (1 + marginRate)
     */
    public double calculateImportCost(double puFob) {
        double puImport = puFob
                * (1 + transportRate)
                * (1 + insuranceRate)
                * (1 + douaneRate)
                * (1 + portRate)
                * (1 + localTransportRate)
                * (1 + marginRate);

        LOGGER.info("FOB: {} -> Import reel: {}", puFob, puImport);
        return puImport;
    }
}
