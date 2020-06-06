package in.projecteka.gateway.common;

import in.projecteka.gateway.common.cache.ServiceOptions;
import org.jeasy.random.EasyRandom;

public class TestBuilders {

    private static final EasyRandom easyRandom = new EasyRandom();

    public static String string() {
        return easyRandom.nextObject(String.class);
    }

    public static ServiceOptions.ServiceOptionsBuilder serviceOptions() {
        return easyRandom.nextObject(ServiceOptions.ServiceOptionsBuilder.class);
    }
}
