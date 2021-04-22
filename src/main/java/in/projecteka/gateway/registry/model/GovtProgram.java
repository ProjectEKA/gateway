package in.projecteka.gateway.registry.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Data
@Builder
public class GovtProgram {
    private String hipId;
    private String name;
}
