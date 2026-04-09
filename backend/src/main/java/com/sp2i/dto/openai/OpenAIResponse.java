package com.sp2i.dto.openai;

import java.util.List;

/**
 * Ce DTO represente une partie utile de la reponse OpenAI.
 *
 * On ne modelise que les champs dont on a besoin ici :
 * - choices
 * - message
 * - content
 */
public record OpenAIResponse(
        List<Choice> choices
) {

    public record Choice(
            Message message
    ) {
    }

    public record Message(
            String content
    ) {
    }
}
