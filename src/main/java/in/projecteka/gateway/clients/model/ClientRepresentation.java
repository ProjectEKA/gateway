package in.projecteka.gateway.clients.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ClientRepresentation {
    String clientId;
    String id;
    List<String> redirectUris;
    Boolean surrogateAuthRequired;
    Boolean enabled;
    Boolean alwaysDisplayInConsole;
    String clientAuthenticatorType;
    Integer notBefore;
    Boolean bearerOnly;
    Boolean consentRequired;
    Boolean standardFlowEnabled;
    Boolean implicitFlowEnabled;
    Boolean directAccessGrantsEnabled;
    Boolean serviceAccountsEnabled;
    Boolean publicClient;
    Boolean frontchannelLogout;
    String protocol;
    Boolean fullScopeAllowed;
    Boolean authorizationServicesEnabled;
}
