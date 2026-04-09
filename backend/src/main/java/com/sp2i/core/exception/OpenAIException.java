package com.sp2i.core.exception;

/**
 * Cette exception represente un probleme lors d'un appel a OpenAI.
 *
 * Pourquoi creer une exception dediee ?
 * - pour separer les erreurs metier des erreurs d'integration externe
 * - pour renvoyer un message plus clair au frontend
 * - pour centraliser le traitement dans le GlobalExceptionHandler
 */
public class OpenAIException extends RuntimeException {

    public OpenAIException(String message) {
        super(message);
    }

    public OpenAIException(String message, Throwable cause) {
        super(message, cause);
    }
}
