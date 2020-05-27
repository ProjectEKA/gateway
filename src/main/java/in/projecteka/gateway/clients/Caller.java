package in.projecteka.gateway.clients;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Caller {
    private String username;
    private Boolean isServiceAccount;
}