package in.projecteka.gateway.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.UUID;

@AllArgsConstructor
@Builder
@Data
public class TraceableMessage {
    String correlationId;
    Object message;

    public String getCorrelationId() {
        return StringUtils.isEmpty(correlationId) ? UUID.randomUUID().toString() : correlationId;
    }
}
