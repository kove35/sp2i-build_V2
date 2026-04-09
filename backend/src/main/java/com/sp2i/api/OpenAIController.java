package com.sp2i.api;

import com.sp2i.core.openai.OpenAIService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Petit controller de test pour verifier l'integration OpenAI.
 */
@RestController
@RequestMapping("/openai")
public class OpenAIController {

    private final OpenAIService openAIService;

    public OpenAIController(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }

    /**
     * Endpoint tres simple pour verifier que la configuration fonctionne.
     */
    @GetMapping("/test")
    public String testOpenAI() {
        return openAIService.callOpenAI("Dis bonjour");
    }
}
