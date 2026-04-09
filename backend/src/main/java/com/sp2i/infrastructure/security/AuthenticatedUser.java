package com.sp2i.infrastructure.security;

import com.sp2i.domain.user.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adaptateur entre notre entite AppUser et Spring Security.
 *
 * Spring Security travaille avec l'interface UserDetails.
 * Cette classe sert donc de pont entre :
 * - notre domaine metier
 * - le moteur de securite
 */
public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;

    public AuthenticatedUser(AppUser user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
    }

    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
