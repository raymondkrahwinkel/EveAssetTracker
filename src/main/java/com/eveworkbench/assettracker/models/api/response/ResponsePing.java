package com.eveworkbench.assettracker.models.api.response;

public class ResponsePing extends ResponseBase {

    public ResponsePing(String message, boolean success) {
        super(message, success);
    }

    public ResponsePing(String message, boolean success, String token) {
        super(message, success);
        this.token = token;
    }

    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
