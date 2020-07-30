package in.projecteka.gateway.common.heartbeat.model;

import lombok.Value;
import lombok.Builder;
import in.projecteka.gateway.clients.model.Error;

import java.time.LocalDateTime;

@Builder
@Value
public class HeartbeatResponse {
    LocalDateTime timeStamp;
    Status status;
    Error error;
}
