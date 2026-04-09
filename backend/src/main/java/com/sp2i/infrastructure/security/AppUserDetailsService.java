package com.sp2i.infrastructure.security;

import com.sp2i.core.exception.BusinessException;
import com.sp2i.infrastructure.persistence.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/**
 * Service charge de retrouver un utilisateur a partir de son email.
 *
 * Spring Security l'utilise pendant l'authentification.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public AppUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return appUserRepository.findByEmail(username)
                .map(AuthenticatedUser::new)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable"));
    }
}
