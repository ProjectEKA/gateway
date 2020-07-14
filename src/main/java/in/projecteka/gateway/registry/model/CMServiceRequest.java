package in.projecteka.gateway.registry.model;

import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
public class CMServiceRequest {
    UUID id;
    String name;
    String url;
    String consentManagerId;
    String cmSuffix;
    Boolean isActive;
    LocalDateTime dateCreated;
    LocalDateTime dateModified;
    Boolean isBlocklisted;
    String license;
    String licenseAuthority;
}
