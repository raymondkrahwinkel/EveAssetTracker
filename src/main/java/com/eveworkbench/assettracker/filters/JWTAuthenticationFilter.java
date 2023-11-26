package com.eveworkbench.assettracker.filters;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.eveworkbench.assettracker.SecurityConstants;
import com.eveworkbench.assettracker.models.database.CharacterDto;
import com.eveworkbench.assettracker.models.request.AuthenticationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

@Deprecated(since = "Not needed because login is handled via eve online", forRemoval = true)
public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private AuthenticationManager authenticationManager;

    public JWTAuthenticationFilter(AuthenticationManager authenticationManager)
    {
        this.authenticationManager = authenticationManager;

        setFilterProcessesUrl("/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            AuthenticationRequest creds = new ObjectMapper().readValue(request.getInputStream(), AuthenticationRequest.class);
            return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    creds.getCode(),
                    null,
                    new ArrayList<>()) // this means no authorities defined
            );
        } catch(IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        // create the JWT token and use the character name as main subject
//        String token = JWT.create()
//                .withSubject(((AuthenticationRequest) authResult.getPrincipal()).getId().toString())
//                .withClaim("id", ((AuthenticationRequest) authResult.getPrincipal()).getId())
//                .withClaim("name", ((AuthenticationRequest) authResult.getPrincipal()).getName())
//                .withClaim("access_token", ((AuthenticationRequest) authResult.getPrincipal()).getAccess_token())
//                .withExpiresAt(new Date(System.currentTimeMillis() + SecurityConstants.EXPIRATION_TIME))
//                .sign(Algorithm.HMAC512(SecurityConstants.SECRET.getBytes()));

        String body = "test";// ((AuthenticationRequest) authResult.getPrincipal()).getName() + " " + token;

        // write the token to the requester
        response.getWriter().write(body);
        response.getWriter().flush();
    }
}
