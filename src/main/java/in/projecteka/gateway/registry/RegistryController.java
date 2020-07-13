package in.projecteka.gateway.registry;

import in.projecteka.gateway.registry.Model.CMServiceRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
public class RegistryController {
    private final RegistryService registryService;

    @PutMapping("/internal/cm")
    public Mono<Void> cmServiceEntries(@RequestBody CMServiceRequest cmServiceRequest) {
        return registryService.populateCMEntry(cmServiceRequest);
    }
}
