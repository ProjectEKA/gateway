package in.projecteka.gateway.common.heartbeat.model;

import lombok.Value;
import lombok.Builder;
import in.projecteka.gateway.clients.model.Error;

@Builder
@Value
public class HeartbeatResponse {
    private String timeStamp;
    private Status status;
    private Error error;
}
