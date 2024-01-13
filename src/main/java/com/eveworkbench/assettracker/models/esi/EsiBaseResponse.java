package com.eveworkbench.assettracker.models.esi;

import java.util.Optional;

public abstract class EsiBaseResponse<T> {
    public String error;
    public Boolean hasError;
    public Integer sso_status;
    public Integer timeout;
    public T value;

    public Boolean contentModified = true;

    public Integer esiErrorLimitRemain;
    public Integer esiErrorLimitReset;

    public Integer statusCode;
    public Integer pages;
    public String etag;

}
