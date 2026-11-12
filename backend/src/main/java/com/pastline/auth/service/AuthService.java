package com.pastline.auth.service;

import com.pastline.auth.entity.User;
import com.pastline.auth.exception.EmailTakenException;
import com.pastline.auth.exception.InvalidTimezoneException;
import com.pastline.auth.repository.UserRepository;
import com.pastline.auth.security.AuthenticatedUser;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private static final String DEFAULT_TIMEZONE = "Europe/Kyiv";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional
    public User register(String email, String rawPassword, String timezone) {
        String zone = timezone == null || timezone.isBlank() ? DEFAULT_TIMEZONE : validZone(timezone);
        String normalized = normalize(email);
        if (users.existsByEmail(normalized)) {
            throw new EmailTakenException();
        }
        return users.save(new User(normalized, passwordEncoder.encode(rawPassword), zone, Instant.now(clock)));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedUser loadUserByUsername(String email) {
        return users.findByEmail(normalize(email))
                .map(u -> new AuthenticatedUser(u.getId(), u.getEmail(), u.getPasswordHash()))
                .orElseThrow(() -> new UsernameNotFoundException("not found"));
    }

    @Transactional(readOnly = true)
    public User get(AuthenticatedUser principal) {
        return users.findById(principal.id()).orElseThrow();
    }

    private static String validZone(String zone) {
        try {
            return ZoneId.of(zone).getId();
        } catch (DateTimeException e) {
            throw new InvalidTimezoneException(zone);
        }
    }

    static String normalize(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }
}
