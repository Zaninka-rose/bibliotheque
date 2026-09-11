package com.ibizabroker.bibliotheque.exceptions;

/**
 * RS-02 / RS-03 : utilisateur identifié par un JWT valide mais sans droits
 * suffisants sur la ressource ciblée. Mappée sur HTTP 403 par
 * {@link GlobalExceptionHandler} — ne jamais confondre avec 401
 * (token absent/invalide), qui est géré par JwtAuthenticationEntryPoint.
 */
public class AccesRefuseException extends RuntimeException {

    public AccesRefuseException(String message) {
        super(message);
    }
}
