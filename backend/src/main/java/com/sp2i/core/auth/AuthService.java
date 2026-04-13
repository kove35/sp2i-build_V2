package com.sp2i.core.auth;

import com.sp2i.core.exception.BusinessException;
import com.sp2i.domain.user.AppUser;
import com.sp2i.dto.auth.AuthResponse;
import com.sp2i.dto.auth.LoginRequest;
import com.sp2i.dto.auth.RegisterRequest;
import com.sp2i.infrastructure.persistence.AppUserRepository;
import com.sp2i.infrastructure.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service metier de l'authentification.
 *
 * Il gere deux cas :
 * - inscription
 * - connexion
 */
@Service
public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private static final String TEST_ADMIN_LOGIN = "admin";
    private static final String TEST_ADMIN_EMAIL = "admin@sp2i.local";

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        validateCredentials(request.getEmail(), request.getPassword());

        if (appUserRepository.findByEmail(request.getEmail().trim().toLowerCase()).isPresent()) {
            throw new BusinessException("Un utilisateur existe deja avec cet email");
        }

        AppUser user = new AppUser();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        AppUser savedUser = appUserRepository.save(user);
        String token = jwtService.generateToken(new AuthenticatedUser(savedUser));

        LOGGER.info("Inscription reussie pour {}", savedUser.getEmail());
        return new AuthResponse(savedUser.getId(), savedUser.getEmail(), token);
    }

    public AuthResponse login(LoginRequest request) {
        validateCredentials(request.getEmail(), request.getPassword());
        String normalizedLogin = normalizeLoginIdentifier(request.getEmail());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizedLogin,
                        request.getPassword()
                )
        );

        AppUser user = appUserRepository.findByEmail(normalizedLogin)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable"));

        String token = jwtService.generateToken(new AuthenticatedUser(user));
        LOGGER.info("Connexion reussie pour {}", user.getEmail());
        return new AuthResponse(user.getId(), user.getEmail(), token);
    }

    private void validateCredentials(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new BusinessException("L'email est obligatoire");
        }

        if (password == null || password.isBlank()) {
            throw new BusinessException("Le mot de passe est obligatoire");
        }
    }

    private String normalizeLoginIdentifier(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        if (TEST_ADMIN_LOGIN.equals(normalizedEmail)) {
            return TEST_ADMIN_EMAIL;
        }

        return normalizedEmail;
    }
}
