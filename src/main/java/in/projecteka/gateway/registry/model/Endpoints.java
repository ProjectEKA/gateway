package in.projecteka.gateway.registry.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@JsonInclude(Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Endpoints {
    List<EndpointDetails> hip_endpoints;
    List<EndpointDetails> hiu_endpoints;
    List<EndpointDetails> health_locker_endpoints;
}
