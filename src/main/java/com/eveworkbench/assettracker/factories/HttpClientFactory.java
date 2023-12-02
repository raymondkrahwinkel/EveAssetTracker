package com.eveworkbench.assettracker.factories;

import org.springframework.stereotype.Service;

import java.net.http.HttpClient;

public interface HttpClientFactory {
    HttpClient create();
}
