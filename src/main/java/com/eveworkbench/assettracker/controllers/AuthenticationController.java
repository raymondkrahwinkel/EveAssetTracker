package com.eveworkbench.assettracker.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.eveworkbench.assettracker.SecurityConstants;
import com.eveworkbench.assettracker.models.database.CharacterDto;
import com.eveworkbench.assettracker.models.esi.OAuthResponse;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@CrossOrigin("http://localhost:4200") // allow requests from the angular frontend
public class AuthenticationController {
    @Value("${esi.clientid}")
    private String clientId;

    @Value("${esi.clientsecret}")
    private String clientSecret;

    @Autowired
    private CharacterRepository characterRepository;

//    AuthenticationManager authenticationManager;

//    public AuthenticationController(AuthenticationManager authenticationManager) {
//        this.authenticationManager = authenticationManager;
//    }

    @GetMapping("/auth/login/url")
    public String getLoginUrl() throws URISyntaxException {
        String callbackUrl = "http://localhost:4200/auth/callback";
        String url = String.format("https://login.eveonline.com/v2/oauth/authorize/?response_type=code&redirect_uri=%s&client_id=%s&state=%s", URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8), clientId, UUID.randomUUID());
        URI uri = new URI(url);
        return uri.toString();
    }

    @GetMapping("/auth/validate")
    public ResponseEntity<?> getValidate(String code, String state)
    {
        // setup the post form data
        Map<String, String> formData = new HashMap<>();
        formData.put("grant_type", "authorization_code");
        formData.put("code", code);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://login.eveonline.com/v2/oauth/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(String.format("%s:%s", clientId, clientSecret).getBytes()))
                    .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(formData)))
                    .build();

            HttpClient httpClient = HttpClient.newHttpClient();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() != 200) {
                return ResponseEntity.badRequest().body("Failed to login to eve online services");
            }

            OAuthResponse oauthResponse = new Gson().fromJson(response.body(), OAuthResponse.class);

            // decode the jwt access_token
            Map<String, Claim> claims = JWT.decode(oauthResponse.access_token).getClaims();

            // get the character name and id from the access token information
            String characterName = claims.get("name").asString();
            Integer characterId;

            Pattern p = Pattern.compile("^CHARACTER:EVE:([0-9]+)$");
            Matcher m = p.matcher(claims.get("sub").asString());
            if(m.find()) {
                characterId = Integer.parseInt(m.group(1));
            } else {
                characterId = null;
            }

            if(characterId == null) {
                return ResponseEntity.badRequest().body("Failed to get character id from login response");
            }

            Optional<CharacterDto> character = characterRepository.findById(characterId); //.orElseThrow(() -> new EntityNotFoundException("Character not found with id: " + characterId));
                character.ifPresentOrElse(
                    (dto) -> {
                        // set the access and refresh token
                        dto.setAccessToken(oauthResponse.access_token);
                        dto.setRefreshToken(oauthResponse.refresh_token);
                        dto.setTokenExpiresAt(new Date(System.currentTimeMillis() + (oauthResponse.expires_in * 1000)));
                        characterRepository.save(dto);
                    },
                    () -> {
                        // create new character
                        CharacterDto dto = new CharacterDto();
                        dto.setId(characterId);
                        dto.setName(characterName);
                        dto.setAccessToken(oauthResponse.access_token);
                        dto.setRefreshToken(oauthResponse.refresh_token);
                        dto.setTokenExpiresAt(new Date(System.currentTimeMillis() + (oauthResponse.expires_in * 1000)));
                        characterRepository.save(dto);
                    });

            // create the JWT token and use the character name as main subject
            // todo : move to central method
            String token = JWT.create()
                    .withSubject(characterId.toString())
                    .withClaim("id", characterId)
                    .withClaim("name", characterName)
                    .withClaim("access_token", oauthResponse.access_token)
                    .withExpiresAt(new Date(System.currentTimeMillis() + SecurityConstants.EXPIRATION_TIME))
                    .sign(Algorithm.HMAC512(SecurityConstants.SECRET.getBytes()));

//            String body = characterName + " " + token;

            boolean test = true;

            return ResponseEntity.ok(characterName + " " + token);
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getFormDataAsString(Map<String, String> formData) {
        StringBuilder formBodyBuilder = new StringBuilder();
        for (Map.Entry<String, String> singleEntry : formData.entrySet()) {
            if (formBodyBuilder.length() > 0) {
                formBodyBuilder.append("&");
            }
            formBodyBuilder.append(URLEncoder.encode(singleEntry.getKey(), StandardCharsets.UTF_8));
            formBodyBuilder.append("=");
            formBodyBuilder.append(URLEncoder.encode(singleEntry.getValue(), StandardCharsets.UTF_8));
        }
        return formBodyBuilder.toString();
    }

    @GetMapping("/auth/login")
    public ResponseEntity<?> getLogin()
    {
        // create authentication token and set it as the current token
//        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken("test", "blep"));
//        SecurityContextHolder.getContext().setAuthentication(authentication);
//
//        // create jwt token
//        CharacterDto character = new CharacterDto();
//        character.setName("Raymondkrah");
//        character.setId(3453434);
//        String jwt = jwtUtil.createToken(character);

        return ResponseEntity.ok("Character data here");
    }
}
