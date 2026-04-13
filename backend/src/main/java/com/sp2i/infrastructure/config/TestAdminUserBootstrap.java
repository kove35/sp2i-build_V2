package com.sp2i.infrastructure.config;

import com.sp2i.domain.user.AppUser;
import com.sp2i.infrastructure.persistence.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Bootstrap d'un compte de test tres simple pour les demos locales et de recette.
 *
 * Identifiants :
 * - login : admin
 * - email stocke : admin@sp2i.local
 * - mot de passe : admin
 */
@Configuration
public class TestAdminUserBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestAdminUserBootstrap.class);
    private static final String TEST_ADMIN_EMAIL = "admin@sp2i.local";
    private static final String TEST_ADMIN_PASSWORD = "admin";

    @Bean
    CommandLineRunner bootstrapTestAdminUser(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            AppUser existingUser = appUserRepository.findByEmail(TEST_ADMIN_EMAIL).orElse(null);

            if (existingUser != null) {
                existingUser.setPassword(passwordEncoder.encode(TEST_ADMIN_PASSWORD));
                appUserRepository.save(existingUser);
                LOGGER.info("Compte de test reinitialise : {} / {}", "admin", TEST_ADMIN_PASSWORD);
                return;
            }

            AppUser testAdminUser = new AppUser();
            testAdminUser.setEmail(TEST_ADMIN_EMAIL);
            testAdminUser.setPassword(passwordEncoder.encode(TEST_ADMIN_PASSWORD));
            appUserRepository.save(testAdminUser);

            LOGGER.info("Compte de test cree : {} / {}", "admin", TEST_ADMIN_PASSWORD);
        };
    }
}
