package com.sp2i.core.dqe;

import org.springframework.stereotype.Service;

/**
 * Service de decision LOCAL / IMPORT / MIX.
 */
@Service
public class DecisionService {

    public String decide(Double localPrice, Double importRenderedPrice, String riskLevel) {
        if (localPrice == null || localPrice <= 0d || importRenderedPrice == null || importRenderedPrice <= 0d) {
            return "MIX";
        }

        double gapRatio = (localPrice - importRenderedPrice) / localPrice;

        if ("ELEVE".equals(riskLevel) && gapRatio < 0.25d) {
            return "LOCAL";
        }
        if (Math.abs(gapRatio) <= 0.05d) {
            return "MIX";
        }
        if (importRenderedPrice < localPrice) {
            return "IMPORT";
        }
        return "LOCAL";
    }
}
