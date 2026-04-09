package com.sp2i.dto.openai;

/**
 * Ce DTO represente un message pour l'API Chat Completions.
 *
 * Exemple :
 * - role = "user"
 * - content = "Dis bonjour"
 */
public record OpenAIMessage(
        String role,
        String content
) {
}
