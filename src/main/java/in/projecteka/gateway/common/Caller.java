package in.projecteka.gateway.common;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.util.List;

@Getter
@Value
@Builder
public class Caller {
    String clientId;
    Boolean isServiceAccount;
    List<Role> roles;
}