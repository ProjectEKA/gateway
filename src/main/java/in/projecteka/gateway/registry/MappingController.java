package in.projecteka.gateway.registry;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@AllArgsConstructor
public class MappingController {
    private MappingService mappingService;

    @GetMapping("/v1/getBridgeUrls")
    public Flux<String> bridgeUrlsController(){
        return mappingService.getUrls();
    }
}
