package in.projecteka.gateway.registry;

import in.projecteka.gateway.registry.model.CMServiceRequest;
import org.jeasy.random.EasyRandom;

public class TestBuilders {
    private static final EasyRandom easyRandom = new EasyRandom();

    public static CMServiceRequest.CMServiceRequestBuilder cmServiceRequest() {
        return easyRandom.nextObject(CMServiceRequest.CMServiceRequestBuilder.class);
    }
}
