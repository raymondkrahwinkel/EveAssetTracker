package com.eveworkbench.assettracker.models.api.response;

public class ResponseBaseWithData<T> extends ResponseBase {
    private T data;

    public ResponseBaseWithData(String message, boolean success, T data) {
        super(message, success);
        this.data = data;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
