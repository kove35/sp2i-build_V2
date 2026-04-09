package com.sp2i.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Cette classe regroupe la configuration OpenAI.
 *
 * Son role est volontairement simple :
 * - lire les proprietes venant de application.yml
 * - exposer ces valeurs via des getters
 * - fournir un RestTemplate pret a l'emploi
 *
 * La cle API n'est jamais ecrite en dur dans le code.
 * Elle doit venir de la variable d'environnement OPENAI_API_KEY.
 */
@Configuration
public class OpenAIConfig {

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.model:gpt-4.1-mini}")
    private String model;

    @Value("${openai.base-url:https://api.openai.com/v1/chat/completions}")
    private String baseUrl;

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Ce bean RestTemplate sera injecte dans le service OpenAI.
     *
     * On configure ici des timeouts raisonnables pour eviter
     * qu'un appel reseau reste bloque trop longtemps.
     */
    @Bean
    public RestTemplate openAiRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(20))
                .setReadTimeout(Duration.ofSeconds(60))
                .build();
    }
}
