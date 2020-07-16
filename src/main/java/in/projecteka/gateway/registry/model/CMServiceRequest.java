package in.projecteka.gateway.registry.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class CMServiceRequest {
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
