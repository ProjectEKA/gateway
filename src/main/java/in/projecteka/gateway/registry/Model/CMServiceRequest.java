package in.projecteka.gateway.registry.Model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
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
