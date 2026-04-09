package com.sp2i.dto.dqe;

public record DqeUploadResultDTO(
        String fileName,
        String documentType,
        String extractedText
) {
}
