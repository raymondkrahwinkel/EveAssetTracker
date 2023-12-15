package com.eveworkbench.assettracker.services;

import com.eveworkbench.assettracker.factories.HttpClientFactory;
import com.eveworkbench.assettracker.factories.HttpClientFactoryImpl;
import com.eveworkbench.assettracker.models.esi.OAuthResponse;
import com.google.gson.Gson;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class EsiService {
    // get the client id from the configuration
    @Value("${esi.clientid}")
    private String clientId;

    // get the client secret from the configuration
    @Value("${esi.clientsecret}")
    private String clientSecret;

    @Autowired
    private HttpClientFactory httpClientFactory;

    // region authentication
    // get the oauth token information
    public Optional<OAuthResponse> getOauthInformation(String code) throws URISyntaxException, InvalidPropertyException {
        // check if the clientid and secret are set
        if(clientId == null || clientId.isEmpty()) {
            throw new InvalidPropertyException(EsiService.class, "clientId", "esi.clientId is not set in the application properties");
        }
        if(clientSecret == null || clientSecret.isEmpty()) {
            throw new InvalidPropertyException(EsiService.class, "clientSecret", "esi.clientSecret is not set in the application properties");
        }

        // check if the code is not empty
        if(code.isEmpty()) {
            throw new IllegalArgumentException("code cannot be empty");
        }

        // setup the post form data
        Map<String, String> formData = new HashMap<>();
        formData.put("grant_type", "authorization_code");
        formData.put("code", code);

        try {
            // create request for authentication
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://login.eveonline.com/v2/oauth/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(String.format("%s:%s", clientId, clientSecret).getBytes()))
                    .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(formData)))
                    .build();

            // execute the created authentication request
            HttpResponse<String> response = httpClientFactory.create()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            // check if the request was successful
            if(response.statusCode() != 200) {
                return Optional.empty();
            }

            OAuthResponse oauthResponse = new Gson().fromJson(response.body(), OAuthResponse.class);
            return Optional.ofNullable(oauthResponse);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            // todo: log error
            return Optional.empty();
        }
    }
    // endregion

    // region support
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
    // endregion
}
