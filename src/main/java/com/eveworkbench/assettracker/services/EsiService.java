package com.eveworkbench.assettracker.services;

import com.eveworkbench.assettracker.factories.HttpClientFactory;
import com.eveworkbench.assettracker.models.database.EsiEtagDto;
import com.eveworkbench.assettracker.models.esi.EsiBaseResponse;
import com.eveworkbench.assettracker.models.esi.OAuthResponse;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.eveworkbench.assettracker.repositories.EsiEtagRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// todo: set default user-agent

@Service
public class EsiService {

    // get the client id from the configuration
    @Value("${esi.clientid}")
    private String clientId;

    // get the client secret from the configuration
    @Value("${esi.clientsecret}")
    private String clientSecret;

    protected final CharacterRepository characterRepository;

    protected final HttpClientFactory httpClientFactory;

    protected final EsiEtagRepository esiEtagRepository;

    protected final Logger logger = LoggerFactory.getLogger(EsiService.class);

    public EsiService(CharacterRepository characterRepository, HttpClientFactory httpClientFactory, EsiEtagRepository esiEtagRepository) {
        this.characterRepository = characterRepository;
        this.httpClientFactory = httpClientFactory;
        this.esiEtagRepository = esiEtagRepository;
    }

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
            logger.error("Exception while getting oauth information from ESI", e);
            return Optional.empty();
        }
    }

    // refresh access token request
    public Optional<OAuthResponse> refreshToken(String refreshToken) {
        // check if the client id and secret are set
        if(clientId == null || clientId.isEmpty()) {
            throw new InvalidPropertyException(EsiService.class, "clientId", "esi.clientId is not set in the application properties");
        }
        if(clientSecret == null || clientSecret.isEmpty()) {
            throw new InvalidPropertyException(EsiService.class, "clientSecret", "esi.clientSecret is not set in the application properties");
        }

        // check if the refreshToken is not empty
        if(refreshToken.isEmpty()) {
            throw new IllegalArgumentException("refreshToken cannot be empty");
        }

        // setup the post form data
        Map<String, String> formData = new HashMap<>();
        formData.put("grant_type", "refresh_token");
        formData.put("refresh_token", refreshToken);

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
            logger.error("Exception while refreshing token from ESI", e);
            return Optional.empty();
        }
    }
    // endregion

    // region support
    protected static String getFormDataAsString(Map<String, String> formData) {
        StringBuilder formBodyBuilder = new StringBuilder();
        for (Map.Entry<String, String> singleEntry : formData.entrySet()) {
            if (!formBodyBuilder.isEmpty()) {
                formBodyBuilder.append("&");
            }
            formBodyBuilder.append(URLEncoder.encode(singleEntry.getKey(), StandardCharsets.UTF_8));
            formBodyBuilder.append("=");
            formBodyBuilder.append(URLEncoder.encode(singleEntry.getValue(), StandardCharsets.UTF_8));
        }
        return formBodyBuilder.toString();
    }

    protected HttpRequest.Builder getBaseCharacterHttpRequestBuilder(String url, Integer characterId) throws URISyntaxException {
        var character = characterRepository.findById(characterId);
        if(character.isEmpty()) {
            throw new RuntimeException("Cannot get character with id: " + characterId);
        }

        return getBaseCharacterHttpRequestBuilder(url, character.get().getAccessToken());
    }

    protected HttpRequest.Builder getBaseCharacterHttpRequestBuilder(String url, String accessToken) throws URISyntaxException {
        // create request for authentication
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Bearer " + accessToken);

        // get the etag information
        Optional<EsiEtagDto> etagDto = esiEtagRepository.findByUrlIgnoreCase(url);
        if(etagDto.isPresent()) {
            builder = builder.header("If-None-Match", etagDto.get().getEtag());
        }

        return builder;
    }

    protected Boolean interpretEsiResponse(EsiBaseResponse<?> response, HttpResponse<String> httpResponse) {
        if(response == null) {
            throw new IllegalArgumentException("ESI Response argument cannot be null");
        }

        if(httpResponse == null) {
            throw new IllegalArgumentException("Http response object cannot be null");
        }

        if(httpResponse.headers() == null) {
            throw new RuntimeException("Http response has no headers");
        }

        // get the base ESI response information
        response.etag = httpResponse.headers().firstValue("ETag").orElse(null);
        response.pages = httpResponse.headers().firstValue("X-Pages").map(Integer::valueOf).orElse(null);
        response.statusCode = httpResponse.statusCode();
        response.contentModified = httpResponse.statusCode() != 304;
        response.esiErrorLimitRemain = httpResponse.headers().firstValue("X-Esi-Error-Limit-Remain").map(Integer::valueOf).orElse(null);
        response.esiErrorLimitReset = httpResponse.headers().firstValue("X-Esi-Error-Limit-Reset").map(Integer::valueOf).orElse(null);

        // check if the status response is ok
        if(httpResponse.statusCode() == 420 /* Error limit */) {
            response.hasError = true;
            response.error = "ESI error limit reached";

            return false;
        }

        // check if there is generic error
        if(!(httpResponse.statusCode() >= 200 && httpResponse.statusCode() < 400)) {
            response.hasError = true;
            response.error = "Error status code received from ESI";

            return false;
        }

        // store the ETag to the database
        Optional<String> etagValue = httpResponse.headers().firstValue("ETag");
        if(etagValue.isPresent()) {
            String etag = etagValue.get();
            if(etag.startsWith("W/")) {
                etag = etag.replace("\"", "");
            }

            String url = httpResponse.uri().toString();
            EsiEtagDto etagDto = esiEtagRepository.findByUrlIgnoreCase(url).orElse(new EsiEtagDto());
            etagDto.setUrl(url);
            etagDto.setEtag(etag);
            esiEtagRepository.save(etagDto);
        }

        return true;
    }
    // endregion
}
