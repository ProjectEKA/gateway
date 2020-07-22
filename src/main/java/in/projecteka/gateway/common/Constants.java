package in.projecteka.gateway.common;

import java.util.HashMap;
import java.util.Map;

public class Constants {
    public static final String API_CALLED = "apiCalled";
    public static final String X_HIP_ID = "X-HIP-ID";
    public static final String X_HIU_ID = "X-HIU-ID";
    public static final String X_CM_ID = "X-CM-ID";
    public static final String REQUEST_ID = "requestId";
    public static final String TIMESTAMP = "timestamp";
    public static final String GW_DEAD_LETTER_EXCHANGE = "gw.dead-letter-exchange";
    public static final String GW_LINK_QUEUE = "gw.link";
    public static final String GW_DATAFLOW_QUEUE = "gw.dataflow";
    public static final Map<String, String> nameMap = new HashMap<>() {
        {
            put(X_HIU_ID, "HIU");
            put(X_CM_ID, "CM");
            put(X_HIP_ID, "HIP");
        }
    };

    // APIs
    public static final String CURRENT_VERSION = "/v0.5";
    public static final String PATH_CARE_CONTEXTS_ON_DISCOVER = CURRENT_VERSION + "/care-contexts/on-discover";
    public static final String PATH_CARE_CONTEXTS_DISCOVER = CURRENT_VERSION + "/care-contexts/discover";
    public static final String PATH_LINK_INIT = CURRENT_VERSION + "/links/link/init";
    public static final String PATH_LINK_ON_INIT = CURRENT_VERSION + "/links/link/on-init";
    public static final String PATH_LINK_CONFIRM = CURRENT_VERSION + "/links/link/confirm";
    public static final String PATH_LINK_ON_CONFIRM = CURRENT_VERSION + "/links/link/on-confirm";
    public static final String PATH_CONSENT_REQUESTS_INIT = CURRENT_VERSION + "/consent-requests/init";
    public static final String PATH_CONSENT_REQUESTS_ON_INIT = CURRENT_VERSION + "/consent-requests/on-init";
    public static final String PATH_CONSENTS_FETCH = CURRENT_VERSION + "/consents/fetch";
    public static final String PATH_CONSENTS_ON_FETCH = CURRENT_VERSION + "/consents/on-fetch";
    public static final String PATH_CONSENTS_HIP_NOTIFY = CURRENT_VERSION + "/consents/hip/notify";
    public static final String PATH_CONSENTS_HIP_ON_NOTIFY = CURRENT_VERSION + "/consents/hip/on-notify";
    public static final String PATH_CONSENTS_HIU_NOTIFY = CURRENT_VERSION + "/consents/hiu/notify";
    public static final String PATH_CONSENTS_HIU_ON_NOTIFY = CURRENT_VERSION + "/consents/hiu/on-notify";
    public static final String PATH_PATIENTS_FIND = CURRENT_VERSION + "/patients/find";
    public static final String PATH_PATIENTS_ON_FIND = CURRENT_VERSION + "/patients/on-find";
    public static final String PATH_HEALTH_INFORMATION_CM_REQUEST = CURRENT_VERSION + "/health-information/cm/request";
    public static final String PATH_HEALTH_INFORMATION_CM_ON_REQUEST = CURRENT_VERSION + "/health-information/cm/on-request";
    public static final String PATH_HEALTH_INFORMATION_HIP_REQUEST = CURRENT_VERSION + "/health-information/hip/request";
    public static final String PATH_HEALTH_INFORMATION_NOTIFY = CURRENT_VERSION + "/health-information/notify";
    public static final String PATH_HEALTH_INFORMATION_HIP_ON_REQUEST = CURRENT_VERSION + "/health-information/hip/on-request";
    public static final String PATH_SESSIONS = CURRENT_VERSION + "/sessions";
    public static final String PATH_WELL_KNOWN_OPENID_CONFIGURATION = CURRENT_VERSION + "/.well-known/openid-configuration";
    public static final String PATH_CERTS = CURRENT_VERSION + "/certs";
    public static final String PATH_HEARTBEAT = CURRENT_VERSION + "/heartbeat";
    public static final String ROUTE_PATH_CM_HEALTH_INFORMATION_REQUEST = CURRENT_VERSION + "/health-information/request";
    public static final String CALLBACK_PATH_HIU_HEALTH_INFORMATION_REQUEST = CURRENT_VERSION + "/health-information/hiu/on-request";
    public static final String CALLBACK_PATH_CM_HEALTH_INFORMATION_REQUEST = CURRENT_VERSION + "/health-information/on-request";
    public static final String PATH_SERVICE_URLS = CURRENT_VERSION + "/service-properties";
    public static final String INTERNAL_BRIDGES = "/internal/bridges";
    public static final String INTERNAL_BRIDGES_BRIDGE_ID_SERVICES = "/internal/bridges/{bridgeId}/services";
    public static final String USER_SESSION = "/user/sessions";
    public static final String INTERNAL_CM = "/internal/cm";


    private Constants() {
    }
}
