package com.sp2i.api;

import com.sp2i.core.exception.BusinessException;
import com.sp2i.core.exception.OpenAIException;
import com.sp2i.dto.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;

/**
 * Ce fichier sert a centraliser la gestion des erreurs pour toute l'API.
 *
 * @RestControllerAdvice signifie :
 * - cette classe surveille les exceptions lancees par les controllers
 * - elle peut transformer une exception Java en reponse JSON propre
 *
 * Avantage :
 * au lieu d'avoir des erreurs differentes partout,
 * on obtient un format unique et facile a comprendre dans Postman.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Logger dedie a la gestion des erreurs.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Gere les erreurs metier.
     *
     * Quand une BusinessException est lancee,
     * on retourne un code HTTP 400 (Bad Request),
     * car la requete envoyee ne respecte pas une regle metier.
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBusinessException(BusinessException exception, HttpServletRequest request) {
        LOGGER.warn("Erreur metier : {}", exception.getMessage());
        return buildErrorResponse(exception.getMessage(), HttpStatus.BAD_REQUEST, request.getRequestURI());
    }

    /**
     * Gere les erreurs d'authentification.
     *
     * Ici, on renvoie 401 car le probleme vient
     * des identifiants ou du token.
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationException(AuthenticationException exception, HttpServletRequest request) {
        LOGGER.warn("Erreur d'authentification : {}", exception.getMessage());
        return buildErrorResponse("Authentification invalide", HttpStatus.UNAUTHORIZED, request.getRequestURI());
    }

    /**
     * Gere les erreurs d'integration avec OpenAI.
     *
     * On renvoie 502 car le backend agit ici comme passerelle
     * vers un service externe.
     */
    @ExceptionHandler(OpenAIException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorResponse handleOpenAIException(OpenAIException exception, HttpServletRequest request) {
        LOGGER.warn("Erreur OpenAI : {}", exception.getMessage());
        return buildErrorResponse(exception.getMessage(), HttpStatus.BAD_GATEWAY, request.getRequestURI());
    }

    /**
     * Gere les fichiers trop volumineux.
     *
     * Cela permet de renvoyer une erreur claire quand un PDF
     * depasse la taille maximale acceptee par le backend.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ErrorResponse handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request
    ) {
        LOGGER.warn("Erreur upload : fichier trop volumineux");
        return buildErrorResponse(
                "Le fichier depasse la taille maximale autorisee.",
                HttpStatus.PAYLOAD_TOO_LARGE,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoResourceFoundException(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        LOGGER.warn("Ressource introuvable : {}", request.getRequestURI());
        return buildErrorResponse(
                "Ressource introuvable.",
                HttpStatus.NOT_FOUND,
                request.getRequestURI()
        );
    }

    /**
     * Gere toutes les autres erreurs inattendues.
     *
     * Ici, on retourne 500, car le probleme vient du serveur
     * et non d'une regle metier.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericException(Exception exception, HttpServletRequest request) {
        LOGGER.error("Erreur technique inattendue", exception);
        return buildErrorResponse(
                "Une erreur interne est survenue.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request.getRequestURI()
        );
    }

    /**
     * Methode utilitaire pour construire un objet ErrorResponse.
     *
     * On rassemble ici les informations communes :
     * - la date
     * - le message
     * - le statut HTTP
     * - le chemin appele
     */
    private ErrorResponse buildErrorResponse(String message, HttpStatus status, String path) {
        return new ErrorResponse(
                LocalDateTime.now(),
                message,
                status.value(),
                path
        );
    }
}
