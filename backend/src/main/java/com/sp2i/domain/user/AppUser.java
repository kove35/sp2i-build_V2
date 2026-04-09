package com.sp2i.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Cette entite represente un utilisateur de la plateforme SP2I Build.
 *
 * Dans un produit SaaS, un utilisateur sert a :
 * - se connecter a l'application
 * - posseder ses projets
 * - isoler ses donnees des autres utilisateurs
 *
 * On stocke ici uniquement le minimum utile pour un premier niveau
 * d'authentification :
 * - un id technique
 * - un email unique
 * - un mot de passe deja hashé
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * L'email sert d'identifiant de connexion.
     *
     * unique = true garantit qu'un meme email ne peut pas etre cree deux fois.
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Le mot de passe stocke ici n'est jamais en clair.
     * Il sera hashé avec BCrypt avant la sauvegarde.
     */
    @Column(nullable = false)
    private String password;

    public AppUser() {
    }

    public AppUser(Long id, String email, String password) {
        this.id = id;
        this.email = email;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
