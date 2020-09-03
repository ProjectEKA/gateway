package in.projecteka.gateway.common;

import in.projecteka.gateway.common.model.Service;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
public class MappingController {
    private static final Logger logger = LoggerFactory.getLogger(MappingController.class);

    private final MappingService mappingService;

    @GetMapping(Constants.PATH_SERVICE_URLS)
    public Mono<Service> fetchAllServiceUrls() {
        logger.info("Request for getting URLs: {}",Constants.PATH_SERVICE_URLS );
        return mappingService.fetchDependentServiceUrls();
    }
}
