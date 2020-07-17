package in.projecteka.gateway.testcommon;

import in.projecteka.gateway.clients.model.RealmRole;
import in.projecteka.gateway.clients.model.ServiceAccount;
import in.projecteka.gateway.common.Caller;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.common.model.ErrorResult;
import in.projecteka.gateway.registry.model.BridgeRegistryRequest;
import in.projecteka.gateway.registry.model.BridgeServiceRequest;
import org.jeasy.random.EasyRandom;

public class TestBuilders {

    private static final EasyRandom easyRandom = new EasyRandom();

    public static String string() {
        return easyRandom.nextObject(String.class);
    }

    public static ServiceOptions.ServiceOptionsBuilder serviceOptions() {
        return easyRandom.nextObject(ServiceOptions.ServiceOptionsBuilder.class);
    }

    public static Caller.CallerBuilder caller() {
        return easyRandom.nextObject(Caller.CallerBuilder.class);
    }

    public static ErrorResult.ErrorResultBuilder errorResult() {
        return easyRandom.nextObject(ErrorResult.ErrorResultBuilder.class);
    }

}
