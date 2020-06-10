package in.projecteka.gateway.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;

import java.util.List;

@AllArgsConstructor
@Getter
@Builder
@ToString
@Value
public class Caller {
    String clientId;
    Boolean isServiceAccount;
    List<Role> roles;
}