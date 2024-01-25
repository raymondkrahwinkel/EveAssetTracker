package com.eveworkbench.assettracker;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class TestUtils {
    public static HttpResponse<String> mockHttpResponse(String url, String body, Integer statusCode) {
        // Create a mock response
        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return statusCode;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                HttpHeaders httpHeaders = mock(HttpHeaders.class);
                when(httpHeaders.firstValue("X-Pages")).thenReturn(Optional.of("1"));
                when(httpHeaders.firstValue("X-Esi-Error-Limit-Remain")).thenReturn(Optional.of("100"));
                when(httpHeaders.firstValue("X-Esi-Error-Limit-Reset")).thenReturn(Optional.of("30"));
                when(httpHeaders.firstValue("ETag")).thenReturn(Optional.of(UUID.randomUUID().toString().replace("-", "")));
                return httpHeaders;
            }

            @Override
            public String body() {
                return body;
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return URI.create(url);
            }

            @Override
            public HttpClient.Version version() {
                return null;
            }
        };
    }
}
