package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Source unique de l'identité de l'appelant : le token JWT validé par
 * JwtRequestFilter et posé dans le SecurityContext.
 *
 * RS-04 : l'identité ne provient JAMAIS du corps de la requête ni d'un
 * paramètre d'URL ; les adhérents ne peuvent s'identifier que par leur token.
 */
@Service
public class AuthenticatedUserService {

    private final UsersRepository usersRepository;

    public AuthenticatedUserService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    /** Username de l'appelant authentifié (subject du JWT). */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Aucune authentification dans le contexte de sécurité");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return principal.toString();
    }

    /** Entité Users de l'appelant, chargée depuis la base via son username. */
    public Users getCurrentUser() {
        return usersRepository.findByUsername(getCurrentUsername())
                .orElseThrow(() -> new IllegalStateException(
                        "Utilisateur authentifié introuvable en base : " + getCurrentUsername()));
    }

    /** Id de l'appelant authentifié. */
    public Integer getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    /** L'appelant a-t-il le rôle donné (ex. "Admin", "Adherent") ? */
    public boolean hasRole(String roleName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + roleName));
    }
}
