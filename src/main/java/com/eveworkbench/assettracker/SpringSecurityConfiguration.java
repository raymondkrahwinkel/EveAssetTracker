package com.eveworkbench.assettracker;

import com.eveworkbench.assettracker.filters.JWTAuthorizationFilter;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.eveworkbench.assettracker.repositories.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SpringSecurityConfiguration {
    @Autowired
    private CharacterRepository characterRepository;
    @Autowired
    private SessionRepository sessionRepository;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> {
            authorizationManagerRequestMatcherRegistry.requestMatchers("/auth/login/url").anonymous();
            authorizationManagerRequestMatcherRegistry.requestMatchers("/auth/validate").anonymous();
//            authorizationManagerRequestMatcherRegistry.requestMatchers("/auth/login").anonymous();

            authorizationManagerRequestMatcherRegistry.requestMatchers("/**").fullyAuthenticated();
        })
//        .addFilter(new JWTAuthenticationFilter(authenticationManager(http.getSharedObject(AuthenticationConfiguration.class))))
        .addFilter(new JWTAuthorizationFilter(authenticationManager(http.getSharedObject(AuthenticationConfiguration.class)), characterRepository, sessionRepository)) // todo: review, the character repository maybe via UserDetailsService
        .csrf(AbstractHttpConfigurer::disable)
        // disable the session creation on the Security layer
        .sessionManagement(session -> {
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        });

        return http.build();
    }
}
