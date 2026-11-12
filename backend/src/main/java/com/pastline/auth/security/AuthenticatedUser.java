package com.pastline.auth.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Принципал у сесії. id — єдине джерело власності записів: userId від клієнта не приймаємо.
 * Хеш пароля потрібен лише під час перевірки входу і стирається після неї.
 */
public final class AuthenticatedUser implements UserDetails, CredentialsContainer {

    private final UUID id;
    private final String email;
    private String passwordHash;

    public AuthenticatedUser(UUID id, String email, String passwordHash) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}
