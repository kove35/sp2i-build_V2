package com.sp2i.dto.openai;

import java.util.List;

/**
 * Ce DTO represente le body JSON envoye a OpenAI.
 */
public record OpenAIRequest(
        String model,
        List<OpenAIMessage> messages
) {
}
