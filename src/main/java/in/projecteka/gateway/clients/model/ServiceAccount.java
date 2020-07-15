package in.projecteka.gateway.clients.model;

import lombok.Value;

import java.util.List;

@Value
public class ServiceAccount {
    String id;
    String createdTimestamp;
    String username;
    boolean enabled;
    boolean totp;
    boolean emailVerified;
    List<String> disableableCredentialTypes;
    List<String> requiredActions;
    Integer notBefore;
}
