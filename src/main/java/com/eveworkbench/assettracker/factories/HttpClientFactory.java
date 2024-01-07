package com.eveworkbench.assettracker.factories;

import java.net.http.HttpClient;

public interface HttpClientFactory {
    HttpClient create();
}
