package com.technogise.leave_management_system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;

@Configuration
public class CsrfConfig {

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @Value("${app.cookie.same.site}")
    private String cookieSameSite;

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> {
            cookie.sameSite(cookieSameSite);
            cookie.secure(cookieSecure);
            cookie.path("/");
        });

        return repository;
    }
}
