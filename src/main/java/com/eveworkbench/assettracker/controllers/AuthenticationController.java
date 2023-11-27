package com.eveworkbench.assettracker.controllers;

import com.eveworkbench.assettracker.services.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@CrossOrigin("http://localhost:4200") // allow requests from the angular frontend
public class AuthenticationController {
    @Value("${esi.clientid}")
    private String clientId;

    @Autowired
    private AuthenticationService authenticationService;

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
}
