package com.dayquest.dayquestbackend.common.mixin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

public abstract class ResponseEntityBodyOnlyMixin {
    @JsonIgnore
    public abstract HttpHeaders getHeaders();

    @JsonIgnore
    public abstract HttpStatus getStatusCode();

    @JsonUnwrapped
    public abstract Object getBody();
}
