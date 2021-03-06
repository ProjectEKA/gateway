package in.projecteka.gateway.registry;

import in.projecteka.gateway.clients.model.FacilitySearchResponse;
import in.projecteka.gateway.clients.model.FindFacilityByIDResponse;
import in.projecteka.gateway.clients.model.HFRFacilityRepresentation;
import in.projecteka.gateway.clients.model.RealmRole;
import in.projecteka.gateway.clients.model.ServiceAccount;
import in.projecteka.gateway.registry.model.Bridge;
import in.projecteka.gateway.registry.model.BridgeRegistryRequest;
import in.projecteka.gateway.registry.model.BridgeService;
import in.projecteka.gateway.registry.model.BridgeServiceRequest;
import in.projecteka.gateway.registry.model.CMServiceRequest;
import in.projecteka.gateway.registry.model.FacilityRepresentation;
import in.projecteka.gateway.registry.model.HFRBridgeResponse;
import in.projecteka.gateway.registry.model.ServiceProfile;
import in.projecteka.gateway.registry.model.ServiceProfileResponse;
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

    public static ServiceProfileResponse.ServiceProfileResponseBuilder serviceProfileResponse() {
        return easyRandom.nextObject(ServiceProfileResponse.ServiceProfileResponseBuilder.class);
    }

    public static ServiceProfile.ServiceProfileBuilder serviceProfile() {
        return easyRandom.nextObject(ServiceProfile.ServiceProfileBuilder.class);
    }

    public static HFRBridgeResponse.HFRBridgeResponseBuilder hfrBridgeResponse() {
        return easyRandom.nextObject(HFRBridgeResponse.HFRBridgeResponseBuilder.class);
    }

    public static FacilityRepresentation.FacilityRepresentationBuilder facilityRepresentationBuilder() {
        return easyRandom.nextObject(FacilityRepresentation.FacilityRepresentationBuilder.class);
    }

    public static FacilitySearchResponse.FacilitySearchResponseBuilder facilitySearchResponseBuilder() {
        return easyRandom.nextObject(FacilitySearchResponse.FacilitySearchResponseBuilder.class);
    }

    public static HFRFacilityRepresentation.HFRFacilityRepresentationBuilder hfrFacilityRepresentationBuilder() {
        return easyRandom.nextObject(HFRFacilityRepresentation.HFRFacilityRepresentationBuilder.class);
    }

    public static FindFacilityByIDResponse.FindFacilityByIDResponseBuilder facilityByIDResponseBuilder() {
        return easyRandom.nextObject(FindFacilityByIDResponse.FindFacilityByIDResponseBuilder.class);
    }
}
