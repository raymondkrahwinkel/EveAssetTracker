package com.eveworkbench.assettracker.models.api.response;

public class ResponseValidate extends ResponseBaseWithData<String> {
    public ResponseValidate(String message, boolean success, String data) {
        super(message, success, data);
    }

    private boolean childCharacterValidation = false;

    public boolean isChildCharacterValidation() {
        return childCharacterValidation;
    }

    public void setChildCharacterValidation(boolean childCharacterValidation) {
        this.childCharacterValidation = childCharacterValidation;
    }
}
