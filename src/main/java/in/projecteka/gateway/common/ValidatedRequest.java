package in.projecteka.gateway.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class ValidatedRequest {
    private final UUID requesterRequestId;
    private final Map<String, Object> deSerializedRequest;
    private final String clientId;
}
