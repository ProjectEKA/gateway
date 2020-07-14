package in.projecteka.gateway.common;

import in.projecteka.gateway.common.model.Path;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
public class MappingController {

    private MappingService mappingService;

    @GetMapping(Constants.GET_BRIDGE_URLS)
    public Mono<Path> bridgeUrlsController() {
        return mappingService.getAllUrls();

    }
}
