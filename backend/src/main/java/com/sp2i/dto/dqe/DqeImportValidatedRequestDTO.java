package com.sp2i.dto.dqe;

import java.util.List;

public record DqeImportValidatedRequestDTO(
        Long projectId,
        List<DqeLineAnalysisDTO> lignes,
        Boolean replaceExisting
) {
}
