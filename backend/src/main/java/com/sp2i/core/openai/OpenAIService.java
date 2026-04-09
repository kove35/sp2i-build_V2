package com.sp2i.core.openai;

import com.sp2i.core.exception.OpenAIException;
import com.sp2i.dto.openai.OpenAIMessage;
import com.sp2i.dto.openai.OpenAIRequest;
import com.sp2i.dto.openai.OpenAIResponse;
import com.sp2i.infrastructure.config.OpenAIConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Ce service isole tous les appels HTTP vers OpenAI.
 *
 * Avantages :
 * - un seul endroit pour gerer les headers et l'URL
 * - un seul endroit pour gerer les erreurs 401 / 429 / reseau
 * - les autres services peuvent simplement appeler callOpenAI(prompt)
 */
@Service
public class OpenAIService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAIService.class);

    private final RestTemplate restTemplate;
    private final OpenAIConfig openAIConfig;

    public OpenAIService(RestTemplate openAiRestTemplate, OpenAIConfig openAIConfig) {
        this.restTemplate = openAiRestTemplate;
        this.openAIConfig = openAIConfig;
    }

    /**
     * Envoie un prompt simple a OpenAI et retourne le texte de reponse.
     */
    public String callOpenAI(String prompt) {
        if (openAIConfig.getApiKey() == null || openAIConfig.getApiKey().isBlank()) {
            throw new OpenAIException("La cle OpenAI est absente. Configure OPENAI_API_KEY dans les variables d'environnement.");
        }

        try {
            LOGGER.info("Appel OpenAI en cours...");

            OpenAIRequest requestBody = new OpenAIRequest(
                    openAIConfig.getModel(),
                    List.of(new OpenAIMessage("user", prompt))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(openAIConfig.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<OpenAIResponse> response = restTemplate.postForEntity(
                    openAIConfig.getBaseUrl(),
                    new HttpEntity<>(requestBody, headers),
                    OpenAIResponse.class
            );

            OpenAIResponse responseBody = response.getBody();
            if (responseBody == null || responseBody.choices() == null || responseBody.choices().isEmpty()) {
                throw new OpenAIException("OpenAI a renvoye une reponse vide.");
            }

            String content = responseBody.choices().get(0).message() == null
                    ? null
                    : responseBody.choices().get(0).message().content();

            if (content == null || content.isBlank()) {
                throw new OpenAIException("Le contenu de la reponse OpenAI est vide.");
            }

            LOGGER.info("Reponse reçue");
            return content;
        } catch (HttpClientErrorException.Unauthorized exception) {
            LOGGER.error("Erreur OpenAI : cle API invalide", exception);
            throw new OpenAIException("OpenAI a refuse l'appel : cle API invalide.", exception);
        } catch (HttpClientErrorException.TooManyRequests exception) {
            LOGGER.error("Erreur OpenAI : quota ou limite atteinte", exception);
            throw new OpenAIException("OpenAI a refuse l'appel : quota ou limite atteinte.", exception);
        } catch (ResourceAccessException exception) {
            LOGGER.error("Erreur OpenAI : probleme reseau", exception);
            throw new OpenAIException("Impossible de joindre OpenAI : probleme reseau ou timeout.", exception);
        } catch (RestClientException exception) {
            LOGGER.error("Erreur OpenAI", exception);
            throw new OpenAIException("Erreur technique pendant l'appel OpenAI.", exception);
        }
    }
}
