package com.dayquest.dayquestbackend.common.mixin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class ResponseEntityMixin<T> {

    @JsonCreator
    public ResponseEntityMixin(
            @JsonProperty("body") T body,
            @JsonProperty("status") HttpStatus status) {
    }

    @JsonCreator
    public ResponseEntityMixin(
            @JsonProperty("body") T body,
            @JsonProperty("headers") HttpHeaders headers,
            @JsonProperty("status") HttpStatus status) {
    }

    @JsonProperty("body")
    public abstract T getBody();

    @JsonProperty("status")
    public abstract HttpStatus getStatusCode();

    @JsonProperty("headers")
    public abstract HttpHeaders getHeaders();
}
