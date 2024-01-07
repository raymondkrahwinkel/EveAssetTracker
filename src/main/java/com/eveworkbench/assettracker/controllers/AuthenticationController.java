package com.eveworkbench.assettracker.controllers;

import com.eveworkbench.assettracker.models.database.CharacterDto;
import com.eveworkbench.assettracker.models.database.SessionDto;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.eveworkbench.assettracker.repositories.SessionRepository;
import com.eveworkbench.assettracker.services.AuthenticationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@RestController
@CrossOrigin
public class AuthenticationController {
    @Value("${esi.clientid:#{null}}")
    private String clientId;

    private final AuthenticationService authenticationService;
    private final CharacterRepository characterRepository;
    private final SessionRepository sessionRepository;

    public AuthenticationController(AuthenticationService authenticationService, CharacterRepository characterRepository, SessionRepository sessionRepository) {
        this.authenticationService = authenticationService;
        this.characterRepository = characterRepository;
        this.sessionRepository = sessionRepository;
    }

    @GetMapping("/auth/login/url")
    public String getLoginUrl() throws URISyntaxException {
        if(clientId == null) {
            throw new RuntimeException("Missing esi client id");
        }

        String callbackUrl = "http://localhost:4200/auth/callback";
        String url = String.format("https://login.eveonline.com/v2/oauth/authorize/?response_type=code&redirect_uri=%s&client_id=%s&state=%s", URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8), clientId, UUID.randomUUID());
        URI uri = new URI(url);
        return uri.toString();
    }

    @GetMapping("/auth/validate")
    public ResponseEntity<?> getValidate(String code) throws Exception
    {
        try {
            String token = authenticationService.validateCharacter(code);
            return ResponseEntity.ok(token);
        }
        catch(RuntimeException e)
        {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/auth/logout")
    public Boolean logout() {
        // get the current logged-in user information
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Integer characterId = Integer.parseInt(auth.getPrincipal().toString());
        String token = auth.getCredentials().toString();

        // get the session information
        Optional<SessionDto> session = sessionRepository.findByCharacterIdAndToken(characterId, token);
        if(session.isEmpty()) {
            return false;
        }

        // remove the sessions
        sessionRepository.delete(session.get());
        return true;
    }

    @PostMapping("/auth/ping")
    public ResponseEntity<String> ping() { // todo: use json object instead of String response
        // get the current logged-in user information
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Integer characterId = Integer.parseInt(auth.getPrincipal().toString());
        String token = auth.getCredentials().toString();

        // get the session information
        Optional<SessionDto> session = sessionRepository.findByCharacterIdAndToken(characterId, token);
        if(session.isEmpty()) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("");
        }

        // get the character information
        Optional<CharacterDto> characterDto = characterRepository.findById(characterId);
        if(characterDto.isEmpty()) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("");
        }

        // check if we need to update the character access token when it is less than 5 minutes valid
        if(characterDto.get().getTokenExpiresAt().before(Date.from(Instant.now().plus(Duration.ofMinutes(5))))) {
            if(!authenticationService.characterRefreshAccessToken(characterId)) {
                return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("");
            }
        }

        String jwtToken = authenticationService.createToken(characterDto.get(), session.get());
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(jwtToken);
    }
}
