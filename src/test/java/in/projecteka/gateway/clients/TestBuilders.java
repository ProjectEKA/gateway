package in.projecteka.gateway.clients;

import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.common.model.ErrorResult;
import org.jeasy.random.EasyRandom;

public class TestBuilders {

    private static final EasyRandom easyRandom = new EasyRandom();

    public static String string() {
        return easyRandom.nextObject(String.class);
    }

    public static ErrorResult.ErrorResultBuilder errorResult() {
        return easyRandom.nextObject(ErrorResult.ErrorResultBuilder.class);
    }

    public static ServiceOptions.ServiceOptionsBuilder serviceOptions() {
        return easyRandom.nextObject(ServiceOptions.ServiceOptionsBuilder.class);
    }
}
