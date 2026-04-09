package com.sp2i.dto.auth;

/**
 * Reponse simple envoyee apres inscription ou connexion.
 *
 * Elle contient le token JWT et quelques informations utiles
 * pour que le frontend sache quel utilisateur est connecte.
 */
public record AuthResponse(
        Long userId,
        String email,
        String token
) {
}
