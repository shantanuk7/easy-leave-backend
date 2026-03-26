package com.technogise.leave_management_system.config;

import com.technogise.leave_management_system.filter.JwtFilter;
import com.technogise.leave_management_system.handler.OAuth2LoginSuccessHandler;
import com.technogise.leave_management_system.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtFilter jwtFilter;
    private final OAuth2LoginSuccessHandler successHandler;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService, JwtFilter jwtFilter,
                          OAuth2LoginSuccessHandler successHandler) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.jwtFilter = jwtFilter;
        this.successHandler = successHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                                    .successHandler(successHandler))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
