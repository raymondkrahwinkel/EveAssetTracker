package com.eveworkbench.assettracker.filters;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.eveworkbench.assettracker.SecurityConstants;
import com.eveworkbench.assettracker.models.database.CharacterDto;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

public class JWTAuthorizationFilter extends BasicAuthenticationFilter {
    private CharacterRepository characterRepository;

    public JWTAuthorizationFilter(AuthenticationManager authenticationManager, CharacterRepository characterRepository) {
        super(authenticationManager);
        this.characterRepository = characterRepository;
    }

    @Override
    // check the incoming authentication header information and when valid login the character in
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        // get the token from the request header
        String header = request.getHeader(SecurityConstants.HEADER_STRING);

        // check if the header value not empty and is valid
        if(header == null || !header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            // stop, push the call forward to the next filter
            chain.doFilter(request, response);
            return;
        }

        // get the authentication token from the header
        UsernamePasswordAuthenticationToken authentication = getAuthentication(request, header);

        // set the authentication token and push to the next filter
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }

    // get authentication token from the authentication header
    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request, String header) {
        if(header != null) {
            DecodedJWT decodedJWT;

            try {
                decodedJWT = JWT.require(Algorithm.HMAC512(SecurityConstants.SECRET.getBytes()))
                        .build()
                        .verify(header.replace(SecurityConstants.TOKEN_PREFIX, ""));
            } catch (TokenExpiredException e) {
                return null;
            }

            // parse the authentication header JWT
            String subject = JWT.require(Algorithm.HMAC512(SecurityConstants.SECRET.getBytes()))
                    .build()
                    .verify(header.replace(SecurityConstants.TOKEN_PREFIX, "")) // remove the Bearer token part from the header
                    .getSubject();

            String characterName = decodedJWT.getClaim("name").asString();
            Integer characterId = decodedJWT.getClaim("id").asInt();
            String accessToken = decodedJWT.getClaim("access_token").asString();
            Date expiresAt = decodedJWT.getExpiresAt();

            if(decodedJWT.getExpiresAt().before(new Date())) {
                // token is expired
               return null;
            }

            // get the character from the database
            Optional<CharacterDto> character = characterRepository.findById(characterId);

            // check if we have a character dto
            if(character.isPresent()) {
                // check if the token is expired
                if(character.get().getTokenExpiresAt() != null
                    && character.get().getAccessToken() != null
                    && character.get().getTokenExpiresAt().before(new Date())
                    && character.get().getAccessToken().equals(accessToken)
                ) {
                    // token is valid, return the token
                    // new empty array, this means no authorities defined
                    return new UsernamePasswordAuthenticationToken(character.get().getId(), character.get().getAccessToken(), new ArrayList<>());
                }
            }

            return null;
        }

        return null;
    }
}
