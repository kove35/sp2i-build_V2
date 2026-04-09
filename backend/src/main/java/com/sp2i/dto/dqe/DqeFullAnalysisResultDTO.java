package com.sp2i.dto.dqe;

import java.util.List;

public record DqeFullAnalysisResultDTO(
        Double scoreGlobal,
        int lignesValides,
        int lignesErreur,
        int lignesSansPrix,
        int lignesSansQuantite,
        int lignesNonClassees,
        int lignesSansBatiment,
        int lignesSansNiveau,
        Double capexTotal,
        Double capexOptimise,
        Double economie,
        List<String> alertes,
        List<DqeAuditBlockDTO> auditBlocs,
        List<DqeLineAnalysisDTO> lignes
) {
}
