package in.projecteka.gateway.common;

import in.projecteka.gateway.common.model.Service;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
public class MappingController {

    private MappingService mappingService;

    @GetMapping(Constants.PATH_SERVICE_URLS)
    public Mono<Service> fetchAllServiceUrls() {
        return mappingService.fetchDependentServiceUrls();

    }
}
