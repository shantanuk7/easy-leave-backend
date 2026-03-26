package com.technogise.leave_management_system.handler;

import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.repository.UserRepository;
import com.technogise.leave_management_system.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final String frontendUrl;
    private final int cookieExpiration;

    OAuth2LoginSuccessHandler(JwtService jwtService, UserRepository userRepository,
                              @Value("${app.frontend-url}") String frontendUrl,
                              @Value("${app.cookie.expiration}") int cookieExpiration) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.frontendUrl = frontendUrl;
        this.cookieExpiration = cookieExpiration;
    }

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        @NonNull Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(user);

        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(cookieExpiration);
        response.addCookie(cookie);

        response.sendRedirect(frontendUrl + "/dashboard");
    }
}
