package in.projecteka.gateway.registry;

import in.projecteka.gateway.clients.model.RealmRole;
import in.projecteka.gateway.clients.model.ServiceAccount;
import in.projecteka.gateway.registry.model.Bridge;
import in.projecteka.gateway.registry.model.BridgeRegistryRequest;
import in.projecteka.gateway.registry.model.BridgeService;
import in.projecteka.gateway.registry.model.BridgeServiceRequest;
import in.projecteka.gateway.registry.model.CMServiceRequest;
import org.jeasy.random.EasyRandom;

public class TestBuilders {
    private static final EasyRandom easyRandom = new EasyRandom();

    public static String string() {
        return easyRandom.nextObject(String.class);
    }

    public static BridgeRegistryRequest.BridgeRegistryRequestBuilder bridgeRegistryRequest() {
        return easyRandom.nextObject(BridgeRegistryRequest.BridgeRegistryRequestBuilder.class);
    }

    public static BridgeServiceRequest.BridgeServiceRequestBuilder bridgeServiceRequest() {
        return easyRandom.nextObject(BridgeServiceRequest.BridgeServiceRequestBuilder.class);
    }

    public static ServiceAccount.ServiceAccountBuilder serviceAccount() {
        return easyRandom.nextObject(ServiceAccount.ServiceAccountBuilder.class);
    }

    public static RealmRole.RealmRoleBuilder realmRole() {
        return easyRandom.nextObject(RealmRole.RealmRoleBuilder.class);
    }

    public static CMServiceRequest.CMServiceRequestBuilder cmServiceRequest() {
        return easyRandom.nextObject(CMServiceRequest.CMServiceRequestBuilder.class);
    }

    public static Bridge.BridgeBuilder bridge() {
        return easyRandom.nextObject(Bridge.BridgeBuilder.class);
    }

    public static BridgeService.BridgeServiceBuilder bridgeService() {
        return easyRandom.nextObject(BridgeService.BridgeServiceBuilder.class);
    }
}
