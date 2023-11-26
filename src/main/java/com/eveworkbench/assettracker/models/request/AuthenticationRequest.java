package com.eveworkbench.assettracker.models.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthenticationRequest {
    private String code;
    private String state;
}
