package com.eveworkbench.assettracker.models.api.response;

public class ResponseSwitch extends ResponsePing {
    public ResponseSwitch(String message, boolean success) {
        super(message, success);
    }

    public ResponseSwitch(String message, boolean success, String token) {
        super(message, success, token);
    }
}
