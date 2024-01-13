package com.eveworkbench.assettracker.models.api.response;

public class ResponseCharacterWallet extends ResponseBaseWithData<Double> {
    private Double difference;

    public ResponseCharacterWallet(String message, boolean success, Double data) {
        super(message, success, data);
    }

    public ResponseCharacterWallet(String message, boolean success, Double data, Double difference) {
        super(message, success, data);
        this.difference = difference;
    }

    public Double getDifference() {
        return difference;
    }

    public void setDifference(Double difference) {
        this.difference = difference;
    }
}
