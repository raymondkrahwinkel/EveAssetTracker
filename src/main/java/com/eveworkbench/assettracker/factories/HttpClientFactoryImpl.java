package com.eveworkbench.assettracker.factories;

import org.springframework.stereotype.Service;

import java.net.http.HttpClient;

@Service
public class HttpClientFactoryImpl implements HttpClientFactory {
    public HttpClient create() {
        return HttpClient.newBuilder().build();
    }
}
