package com.eveworkbench.assettracker.models.api.response;

public class ResponsePing extends ResponseBaseWithData<String> {

    public ResponsePing(String message, boolean success) {
        super(message, success, null);
    }

    public ResponsePing(String message, boolean success, String token) {
        super(message, success, token);
    }
}
