package com.eveworkbench.assettracker;

public class SecurityConstants {
    public static final String SECRET = "mysecret"; // todo: move to configuration

    public static final Integer EXPIRATION_TIME = ((15 * 60) * 1000); // 15 minutes
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
}
