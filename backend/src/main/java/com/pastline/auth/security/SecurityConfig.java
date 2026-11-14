package com.pastline.auth.security;

import com.pastline.shared.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import tools.jackson.databind.ObjectMapper;

/**
 * Серверна сесія в HttpOnly cookie; CSRF-токен у cookie XSRF-TOKEN, React повертає його в заголовку X-XSRF-TOKEN.
 * Без JWT, form login і httpBasic: вхід лише через POST /api/v1/auth/login.
 */
@Configuration
public class SecurityConfig {

    /** Лише у веб-застосунку: профіль benchmark працює без веб-сервера, і HttpSecurity там немає. */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, ObjectMapper mapper, SecurityContextRepository securityContextRepository)
            throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/actuator/health", "/api/v1/auth/csrf", "/api/v1/auth/register", "/api/v1/auth/login")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .securityContext(c -> c.securityContextRepository(securityContextRepository))
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // Без маскування токена: значення з cookie можна покласти в заголовок як є
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> write(
                                res,
                                mapper,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                ErrorResponse.of("UNAUTHORIZED", "Потрібна автентифікація")))
                        .accessDeniedHandler((req, res, ex) -> write(
                                res,
                                mapper,
                                HttpServletResponse.SC_FORBIDDEN,
                                ex instanceof CsrfException
                                        ? ErrorResponse.of("CSRF_INVALID", "Відсутній або недійсний CSRF-токен")
                                        : ErrorResponse.of("ACCESS_DENIED", "Доступ заборонено"))));
        return http.build();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder(); // bcrypt
    }

    @Bean
    AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    private static void write(HttpServletResponse response, ObjectMapper mapper, int status, ErrorResponse body)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), body);
    }
}
