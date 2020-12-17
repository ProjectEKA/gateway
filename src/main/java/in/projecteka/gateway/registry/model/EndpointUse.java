package in.projecteka.gateway.registry.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EndpointUse {

    REGISTRATION("registration"),
    DATA_UPLOAD("data-upload");
    private final String endPointUse;

    EndpointUse(String use) {
        endPointUse = use;
    }

    @JsonValue
    public String getValue() {
        return endPointUse;
    }
}
