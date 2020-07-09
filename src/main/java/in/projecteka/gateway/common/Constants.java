package in.projecteka.gateway.common;

public class Constants {
    public static final String API_CALLED = "apiCalled";
    public static final String X_HIP_ID = "X-HIP-ID";
    public static final String X_HIU_ID = "X-HIU-ID";
    public static final String X_CM_ID = "X-CM-ID";
    public static final String REQUEST_ID = "requestId";
    public static final String GW_DEAD_LETTER_EXCHANGE = "gw.dead-letter-exchange";
    public static final String GW_LINK_QUEUE = "gw.link";
    public static final String GW_DATAFLOW_QUEUE = "gw.dataflow";

    // APIs
    public static final String V_1_CARE_CONTEXTS_ON_DISCOVER = "/v1/care-contexts/on-discover";
    public static final String V_1_CARE_CONTEXTS_DISCOVER = "/v1/care-contexts/discover";
    public static final String V_1_LINKS_LINK_INIT = "/v1/links/link/init";
    public static final String V_1_LINKS_LINK_ON_INIT = "/v1/links/link/on-init";
    public static final String V_1_LINKS_LINK_CONFIRM = "/v1/links/link/confirm";
    public static final String V_1_LINKS_LINK_ON_CONFIRM = "/v1/links/link/on-confirm";
    public static final String V_1_CONSENT_REQUESTS_INIT = "/v1/consent-requests/init";
    public static final String V_1_CONSENT_REQUESTS_ON_INIT = "/v1/consent-requests/on-init";
    public static final String V_1_CONSENTS_FETCH = "/v1/consents/fetch";
    public static final String V_1_CONSENTS_ON_FETCH = "/v1/consents/on-fetch";
    public static final String V_1_CONSENTS_HIP_NOTIFY = "/v1/consents/hip/notify";
    public static final String V_1_CONSENTS_HIP_ON_NOTIFY = "/v1/consents/hip/on-notify";
    public static final String V_1_CONSENTS_HIU_NOTIFY = "/v1/consents/hiu/notify";
    public static final String V_1_PATIENTS_FIND = "/v1/patients/find";
    public static final String V_1_PATIENTS_ON_FIND = "/v1/patients/on-find";
    public static final String V_1_HEALTH_INFORMATION_CM_REQUEST = "/v1/health-information/cm/request";
    public static final String V_1_HEALTH_INFORMATION_CM_ON_REQUEST = "/v1/health-information/cm/on-request";
    public static final String V_1_HEALTH_INFORMATION_HIP_REQUEST = "/v1/health-information/hip/request";
    public static final String V_1_HEALTH_INFORMATION_NOTIFY = "/v1/health-information/notify";
    public static final String V_1_HEALTH_INFORMATION_HIP_ON_REQUEST = "/v1/health-information/hip/on-request";
    public static final String V_1_SESSIONS = "/v1/sessions";
    public static final String V_1_WELL_KNOWN_OPENID_CONFIGURATION = "/v1/.well-known/openid-configuration";
    public static final String V_1_CERTS = "/v1/certs";
    public static final String V_1_HEARTBEAT = "/v1/heartbeat";

    private Constants() {
    }
}
