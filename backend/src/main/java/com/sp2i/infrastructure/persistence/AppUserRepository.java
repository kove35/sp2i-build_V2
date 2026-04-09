package com.sp2i.infrastructure.persistence;

import com.sp2i.domain.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository de lecture/ecriture des utilisateurs.
 *
 * On ajoute findByEmail(...) car l'email est l'identifiant
 * principal utilise pendant la connexion.
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);
}
