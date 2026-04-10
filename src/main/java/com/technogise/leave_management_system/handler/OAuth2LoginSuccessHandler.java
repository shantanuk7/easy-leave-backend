package com.technogise.leave_management_system.handler;

import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.UserRepository;
import com.technogise.leave_management_system.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Slf4j
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final CsrfTokenRepository csrfTokenRepository;

    @Value("${app.cookie.expiration}")
    private int cookieExpiration;

    @Value("${app.redirect.frontend.url}")
    private String redirectFrontendUrl;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @Value("${app.cookie.same.site}")
    private String cookieSameSite;

    OAuth2LoginSuccessHandler(
            UserRepository userRepository,
            JwtService jwtService,
            CsrfTokenRepository csrfTokenRepository
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        @NonNull Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        log.info("OAuth2 login:  for email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("OAuth2 login failed — user not found for email={}", email);
                    return new HttpException(HttpStatus.NOT_FOUND, "User not found");
                });

        String token = jwtService.generateToken(user);
        log.debug("JWT generated for userId={}, email={}", user.getId(), email);

        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(cookieExpiration);
        cookie.setAttribute("SameSite", cookieSameSite);

        response.addCookie(cookie);
        CsrfToken csrfToken = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(csrfToken, request, response);

        log.info("OAuth2 login successful for userId={}, email={}, redirecting to {}",
                user.getId(), email, redirectFrontendUrl);
        response.sendRedirect(redirectFrontendUrl);
    }
}
