package in.projecteka.gateway.clients.model;

import lombok.Value;

@Value
public class RealmRole {
    String id;
    String name;
    boolean composite;
    boolean clientRole;
    String containerId;
}
