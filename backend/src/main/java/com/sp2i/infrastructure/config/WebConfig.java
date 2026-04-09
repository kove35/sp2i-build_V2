package com.sp2i.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Ce fichier sert a autoriser le frontend React local
 * a appeler le backend Spring Boot depuis le navigateur.
 *
 * Pourquoi cette classe est utile ?
 * Un frontend Vite tourne souvent sur http://localhost:5173
 * alors que le backend tourne sur http://localhost:8080.
 *
 * Pour le navigateur, ce sont deux origines differentes.
 * Sans configuration CORS, le navigateur bloque les requetes.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Cette methode declare les regles CORS du projet.
     *
     * Ici, on autorise le frontend local a appeler :
     * - GET pour lire les donnees
     * - POST pour creer des donnees
     *
     * allowedHeaders("*") simplifie le developpement local :
     * le frontend peut envoyer les en-tetes HTTP standards sans blocage.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
