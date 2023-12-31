package com.eveworkbench.assettracker.controllers;

import com.eveworkbench.assettracker.models.database.CharacterDto;
import com.eveworkbench.assettracker.models.database.SessionDto;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.eveworkbench.assettracker.repositories.SessionRepository;
import com.eveworkbench.assettracker.services.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@RestController
@CrossOrigin("http://localhost:4200") // allow requests from the angular frontend
public class AuthenticationController {
    @Value("${esi.clientid}")
    private String clientId;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @GetMapping("/auth/login/url")
    public String getLoginUrl() throws URISyntaxException {
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

    @PostMapping("/auth/ping")
    public ResponseEntity<String> ping() {
        // get the current logged in user information
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Integer characterId = Integer.parseInt(auth.getPrincipal().toString());
        String token = auth.getCredentials().toString();

        // get the session information
        Optional<SessionDto> session = sessionRepository.findByCharacterIdAndToken(characterId, token);
        if(session.isEmpty()) {
            return ResponseEntity.ok("");
        }

        // get the character information
        Optional<CharacterDto> characterDto = characterRepository.findById(characterId);
        if(characterDto.isEmpty()) {
            return ResponseEntity.ok("");
        }

        // check if we need to update the character access token
        // todo: add check and update trigger when less then 5 minutes valid

        // update the session expire timer
        // todo: only update if the token is 5 minutes or less valid
        String jwtToken = authenticationService.createToken(characterDto.get(), session.get());

        return ResponseEntity.ok(jwtToken);
    }
}
