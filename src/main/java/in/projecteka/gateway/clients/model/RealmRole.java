package in.projecteka.gateway.clients.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RealmRole {
    String id;
    String name;
    boolean composite;
    boolean clientRole;
    String containerId;
}
