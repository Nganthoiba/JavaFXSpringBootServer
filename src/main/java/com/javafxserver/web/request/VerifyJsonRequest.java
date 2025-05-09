package com.javafxserver.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VerifyJsonRequest {
	@JsonProperty("payload")
    public String payload;

    @JsonProperty("signature")
    public String signature;
}
