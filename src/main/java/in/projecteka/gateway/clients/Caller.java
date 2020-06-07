package in.projecteka.gateway.clients;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class Caller {
    private final String clientId;
    private final Boolean isServiceAccount;
}