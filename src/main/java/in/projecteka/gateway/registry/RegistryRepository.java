package in.projecteka.gateway.registry;

import in.projecteka.gateway.clients.ClientError;
import in.projecteka.gateway.common.DbOperationError;
import in.projecteka.gateway.registry.model.Bridge;
import in.projecteka.gateway.registry.model.BridgeRegistryRequest;
import in.projecteka.gateway.registry.model.BridgeService;
import in.projecteka.gateway.registry.model.CMEntry;
import in.projecteka.gateway.registry.model.CMServiceRequest;
import in.projecteka.gateway.registry.model.EndpointDetails;
import in.projecteka.gateway.registry.model.Endpoints;
import in.projecteka.gateway.registry.model.FacilityRepresentation;
import in.projecteka.gateway.registry.model.HFRBridgeResponse;
import in.projecteka.gateway.registry.model.ServiceDetailsResponse;
import in.projecteka.gateway.registry.model.ServiceProfile;
import in.projecteka.gateway.registry.model.ServiceRole;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import static in.projecteka.gateway.common.Serializer.from;
import static in.projecteka.gateway.common.Serializer.to;
import static in.projecteka.gateway.registry.ServiceType.HEALTH_LOCKER;
import static in.projecteka.gateway.registry.ServiceType.HIP;
import static in.projecteka.gateway.registry.ServiceType.HIU;


@AllArgsConstructor
public class RegistryRepository {
    private static final Logger logger = LoggerFactory.getLogger(RegistryRepository.class);

    private static final String SELECT_CM = "SELECT * FROM consent_manager where suffix = $1";
    private static final String CREATE_CM_ENTRY =
            "INSERT INTO consent_manager (name, url, cm_id, suffix, active, blocklisted, license, licensing_authority)"
                    + "VALUES ($1, $2, $3, $3, $4, $5, '', '')";
    private static final String UPDATE_CM_ENTRY =
            "UPDATE consent_manager SET name = $1, url = $2, active = $3, blocklisted = $4," +
                    " date_modified = timezone('utc'::text, now()) " +
                    "WHERE consent_manager.suffix = $5";

    private static final String SELECT_BRIDGE = "SELECT name, url, bridge_id, active, blocklisted FROM bridge " +
            "WHERE bridge_id = $1";
    private static final String INSERT_BRIDGE_ENTRY = "INSERT INTO " +
            "bridge (name, url, bridge_id, active, blocklisted) VALUES ($1, $2, $3, $4, $5)";
    private static final String UPDATE_BRIDGE_ENTRY = "UPDATE bridge SET name = $1, url = $2, active = $3, " +
            "blocklisted = $4, date_modified = timezone('utc'::text, now()) WHERE bridge.bridge_id = $5";

    private static final String SELECT_BRIDGE_SERVICE = "SELECT service_id FROM bridge_service " +
            "WHERE bridge_id = $1 AND service_id = $2";
    private static final String SELECT_BRIDGE_SERVICES = "SELECT service_id, is_hip, is_hiu, is_health_locker FROM bridge_service " +
            "WHERE bridge_id = $1 AND active = $2";
    private static final String SELECT_BRIDGE_SERVICES_BY_SERVICE_ID = "SELECT service_id, name, is_hip, is_hiu," +
            " is_health_locker, active, endpoints FROM bridge_service WHERE service_id = $1 AND active = $2";
    private static final String SELECT_BRIDGE_PROFILE = "SELECT name, url, bridge_id, active, blocklisted, " +
            "date_created, date_modified FROM bridge WHERE bridge_id = $1";
    private static final String SELECT_ENDPOINTS_OF_SERVICE = "SELECT endpoints FROM bridge_service " +
            "WHERE bridge_id = $1 AND service_id = $2";

    private static final String SELECT_FACILITIES_BY_NAME = "SELECT service_id, name, is_hip, is_hiu, is_health_locker " +
            "FROM bridge_service WHERE UPPER(name) LIKE $1 AND is_hip = true";

    private final PgPool readWriteClient;
    private final PgPool readOnlyClient;

    public Mono<Bridge> ifPresent(String bridgeId) {
        return Mono.create(monoSink -> this.readOnlyClient.preparedQuery(SELECT_BRIDGE)
                .execute(Tuple.of(bridgeId),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to fetch bridge"));
                                return;
                            }
                            var iterator = handler.result().iterator();
                            if (!iterator.hasNext()) {
                                monoSink.success(Bridge.builder().build());
                                return;
                            }
                            var row = iterator.next();
                            var bridge = Bridge.builder()
                                    .id(row.getString("bridge_id"))
                                    .name(row.getString("name"))
                                    .url(row.getString("url"))
                                    .active(row.getBoolean("active"))
                                    .blocklisted(row.getBoolean("blocklisted"))
                                    .build();
                            monoSink.success(bridge);
                        }));
    }

    public Mono<CMEntry> getCMEntryIfActive(String suffix) {
        return Mono.create(monoSink -> readOnlyClient.preparedQuery(SELECT_CM)
                .execute(Tuple.of(suffix), handler -> {
                    if (handler.failed()) {
                        logger.error(handler.cause().getMessage(), handler.cause());
                        monoSink.error(new DbOperationError("Failed to get the CM entry"));
                        return;
                    }
                    var iterator = handler.result().iterator();
                    if (!iterator.hasNext()) {
                        monoSink.success(CMEntry.builder().isExists(false).build());
                        return;
                    }
                    monoSink.success(cmEntryFrom(iterator.next()));
                }));
    }

    private CMEntry cmEntryFrom(Row row) {
        return CMEntry.builder()
                .isExists(true)
                .isActive(row.getBoolean("active"))
                .build();
    }

    public Mono<Void> createCMEntry(CMServiceRequest request) {
        return Mono.create(monoSink -> readWriteClient.preparedQuery(CREATE_CM_ENTRY)
                .execute(Tuple.of(request.getName(), request.getUrl(), request.getSuffix(),
                        request.getIsActive(), request.getIsBlocklisted()),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to create CM entry"));
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<Void> updateCMEntry(CMServiceRequest request) {
        return Mono.create(monoSink -> readWriteClient.preparedQuery(UPDATE_CM_ENTRY)
                .execute(Tuple.of(request.getName(), request.getUrl(), request.getIsActive(),
                        request.getIsBlocklisted(), request.getSuffix()),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to update CM entry"));
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<Void> insertBridgeEntry(BridgeRegistryRequest request) {
        return Mono.create(monoSink -> readWriteClient.preparedQuery(INSERT_BRIDGE_ENTRY)
                .execute(Tuple.of(request.getName(), request.getUrl(), request.getId(),
                        request.getActive(), request.getBlocklisted()),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to insert bridge entry"));
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<Void> updateBridgeEntry(BridgeRegistryRequest request) {
        return Mono.create(monoSink -> readWriteClient.preparedQuery(UPDATE_BRIDGE_ENTRY)
                .execute(Tuple.of(request.getName(), request.getUrl(),
                        request.getActive(), request.getBlocklisted(), request.getId()),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to update bridge entry"));
                                return;
                            }
                            monoSink.success();
                        }));
    }

    private String prepareSelectActiveBridgeServiceQuery(String typeColumnName) {
        return "SELECT service_id FROM bridge_service WHERE service_id = $1 AND " + typeColumnName + " = $2 AND bridge_id != $3";
    }

    public Mono<Boolean> ifPresent(String serviceId, ServiceType type, boolean active, String bridgeId) {
        return select(prepareSelectActiveBridgeServiceQuery(getColumnName(type)),
                Tuple.of(serviceId, active, bridgeId),
                "Failed to fetch active bridge service");
    }

    public Mono<Boolean> ifBridgeServicePresent(String bridgeId, String serviceId) {
        return select(SELECT_BRIDGE_SERVICE,
                Tuple.of(bridgeId, serviceId),
                "Failed to fetch bridge service");
    }

    private String prepareInsertBridgeServiceQuery(Map<ServiceType, Boolean> typeActive) {
        StringBuilder typeColumnNames = new StringBuilder();
        StringBuilder typeValues = new StringBuilder();
        for(Entry<ServiceType, Boolean> entry : typeActive.entrySet()) {
            String colName = ", " + getColumnName(entry.getKey());
            String colVal = ", " + entry.getValue();
            typeColumnNames.append(colName);
            typeValues.append(colVal);
        }
        return "INSERT INTO bridge_service (bridge_id, service_id, name, active, endpoints" + typeColumnNames + ") VALUES ($1, $2, $3, $4, $5" + typeValues + ")";
    }

    public Mono<Void> insertBridgeServiceEntry(String bridgeId, String serviceId, String serviceName, Endpoints endpoints, Map<ServiceType, Boolean> typeActive) {
        return Mono.create(monoSink -> readWriteClient.preparedQuery(prepareInsertBridgeServiceQuery(typeActive))
                .execute(Tuple.of(bridgeId, serviceId, serviceName, true, new JsonObject(from(endpoints).get())),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to insert bridge service entry"));
                                return;
                            }
                            monoSink.success();
                        }));
    }

    private String prepareUpdateBridgeServiceQuery(Map<ServiceType, Boolean> typeActive) {
        StringBuilder setTypeColumnValues = new StringBuilder();
        for(Entry<ServiceType, Boolean> entry : typeActive.entrySet()) {
            String result = getColumnName(entry.getKey()) + " = " + entry.getValue() + ", ";
            setTypeColumnValues.append(result);
        }

        return "UPDATE bridge_service SET name = $2, endpoints = $6, " + setTypeColumnValues.toString() +
                "date_modified = timezone('utc'::text, now()) FROM bridge " +
                "WHERE bridge_service.bridge_id = bridge.bridge_id AND bridge_service.bridge_id = $1 " +
                "AND bridge.active = $3 AND bridge_service.service_id = $4 AND bridge_service.active = $5";
    }

    public Mono<Void> updateBridgeServiceEntry(String bridgeId, String serviceId, String serviceName, Endpoints endpoints, Map<ServiceType, Boolean> typeActive) {
        return Mono.create(monoSink -> readWriteClient.preparedQuery(prepareUpdateBridgeServiceQuery(typeActive))
                .execute(Tuple.of(bridgeId, serviceName, true, serviceId, true, new JsonObject(from(endpoints).get())),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to update bridge service entry"));
                                return;
                            }
                            monoSink.success();
                        }));
    }

    private Mono<Boolean> select(String query, Tuple params, String errorMessage) {
        return Mono.create(monoSink -> this.readOnlyClient.preparedQuery(query)
                .execute(params,
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError(errorMessage));
                                return;
                            }
                            var iterator = handler.result().iterator();
                            if (!iterator.hasNext()) {
                                monoSink.success(false);
                                return;
                            }
                            monoSink.success(true);
                        }));
    }

    public Flux<BridgeService> fetchBridgeServicesIfPresent(String bridgeId) {
        return Flux.create(fluxSink -> this.readOnlyClient.preparedQuery(SELECT_BRIDGE_SERVICES)
                .execute(Tuple.of(bridgeId, true),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                fluxSink.error(new DbOperationError("Failed to fetch bridge services"));
                                return;
                            }
                            RowSet<Row> results = handler.result();
                            if (results.iterator().hasNext()) {
                                results.forEach(row -> {
                                    var isHIU = Boolean.TRUE.equals(row.getBoolean("is_hiu"));
                                    var isHIP = Boolean.TRUE.equals(row.getBoolean("is_hip"));
                                    var isLocker = Boolean.TRUE.equals(row.getBoolean("is_health_locker"));

                                    ServiceType type = null;
                                    if(isHIU) type = HIU;
                                    if(isHIP) type = HIP;
                                    if(isLocker) type = HEALTH_LOCKER;

                                    if (Objects.isNull(type))
                                        fluxSink.error(ClientError.serviceTypeNoAssignedToService());

                                    fluxSink.next(BridgeService.builder()
                                            .id(row.getString("service_id"))
                                            .type(type)
                                            .build());
                                });
                            }
                            fluxSink.complete();
                        }));
    }

    public Mono<ServiceProfile> fetchServiceEntries(String serviceId) {
        return Mono.create(monoSink -> this.readOnlyClient.preparedQuery(SELECT_BRIDGE_SERVICES_BY_SERVICE_ID)
                .execute(Tuple.of(serviceId, true),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to fetch services by service id"));
                                return;
                            }
                            RowSet<Row> results = handler.result();
                            List<ServiceType> types = new ArrayList<>();
                            Endpoints endpoints = new Endpoints();
                            final ServiceProfile.ServiceProfileBuilder[] serviceProfile = new ServiceProfile.ServiceProfileBuilder[1];
                            if (results.iterator().hasNext()) {
                                results.forEach(row -> {
                                    serviceProfile[0] = ServiceProfile.builder()
                                            .id(row.getString("service_id"))
                                            .name(row.getString("name"))
                                            .active(row.getBoolean("active"));
                                    Object endpointJson = row.getValue("endpoints");
                                    Endpoints endpointsObj = Endpoints.builder().build();
                                    if(endpointJson != null) {
                                        endpointsObj = to(endpointJson);
                                    }
                                    var isHip = row.getBoolean("is_hip");
                                    var isHiu = row.getBoolean("is_hiu");
                                    var isHealthLocker = row.getBoolean("is_health_locker");
                                    if (Boolean.TRUE.equals(isHip)) {
                                        types.add(HIP);
                                        endpoints.setHipEndpoints(endpointsObj.getHipEndpoints());
                                    }
                                    if (Boolean.TRUE.equals(isHiu)) {
                                        types.add(HIU);
                                        endpoints.setHiuEndpoints(endpointsObj.getHiuEndpoints());

                                    }
                                    if (Boolean.TRUE.equals(isHealthLocker)) {
                                        types.add(ServiceType.HEALTH_LOCKER);
                                        endpoints.setHealthLockerEndpoints(endpointsObj.getHealthLockerEndpoints());
                                    }
                                });
                                serviceProfile[0].types(types);
                                serviceProfile[0].endpoints(endpoints);
                            } else {
                                monoSink.success();
                                return;
                            }
                            monoSink.success(serviceProfile[0].build());
                        }));
    }

    private String prepareSelectBridgeServicesOfTypeQuery(String typeColumnName) {
        return "SELECT service_id, name, active, endpoints FROM bridge_service WHERE " + typeColumnName + " = $1 AND active = $2";
    }

    public Mono<List<ServiceDetailsResponse>> fetchServicesOfType(String serviceType) {
        return Mono.create(monoSink -> this.readOnlyClient
                .preparedQuery(prepareSelectBridgeServicesOfTypeQuery(getColumnName(ServiceType.valueOf(serviceType))))
                .execute(Tuple.of(true, true),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to fetch services by type"));
                                return;
                            }
                            RowSet<Row> rowSet = handler.result();
                            List<ServiceDetailsResponse> results = new ArrayList<>();
                            if (rowSet.iterator().hasNext()) {
                                rowSet.forEach(row -> {
                                    Object endpointJson = row.getValue("endpoints");
                                    Endpoints endpoints = new Endpoints();
                                    if(endpointJson != null) {
                                        endpoints = to(endpointJson);
                                    }
                                    List<EndpointDetails> endpointsSpecificToType;
                                    switch (ServiceType.valueOf(serviceType)) {
                                        case HIP : endpointsSpecificToType = endpoints.getHipEndpoints();
                                        break;
                                        case HIU : endpointsSpecificToType = endpoints.getHiuEndpoints();
                                        break;
                                        default : endpointsSpecificToType = endpoints.getHealthLockerEndpoints();
                                    }
                                    results.add(ServiceDetailsResponse.builder()
                                            .id(row.getString("service_id"))
                                            .name(row.getString("name"))
                                            .active(row.getBoolean("active"))
                                            .type(ServiceRole.valueOf(serviceType))
                                            .endpoints(endpointsSpecificToType)
                                            .build());
                                });
                            }
                            monoSink.success(results);
                        })
        );
    }

    private static String getColumnName(ServiceType serviceType) {
        return "is_" + serviceType.toString().toLowerCase();
    }

    public Mono<HFRBridgeResponse> bridgeProfile(String bridgeId) {
        return Mono.create(monoSink -> this.readOnlyClient.preparedQuery(SELECT_BRIDGE_PROFILE)
                .execute(Tuple.of(bridgeId),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to fetch bridge profile"));
                                return;
                            }
                            var iterator = handler.result().iterator();
                            if (!iterator.hasNext()) {
                                monoSink.success();
                                return;
                            }
                            var row = iterator.next();
                            var bridgeProfile = HFRBridgeResponse.builder()
                                    .id(row.getString("bridge_id"))
                                    .name(row.getString("name"))
                                    .url(row.getString("url"))
                                    .active(row.getBoolean("active"))
                                    .blocklisted(row.getBoolean("blocklisted"))
                                    .createdAt(row.getLocalDateTime("date_created"))
                                    .modifiedAt(row.getLocalDateTime("date_modified"))
                                    .build();
                            monoSink.success(bridgeProfile);
                        }));
    }

    public Mono<Endpoints> fetchExistingEndpoints(String bridgeId, String serviceId) {
        return Mono.create(monoSink -> this.readOnlyClient.preparedQuery(SELECT_ENDPOINTS_OF_SERVICE)
                .execute(Tuple.of(bridgeId, serviceId),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to fetch endpoints from bridge service"));
                                return;
                            }
                            var iterator = handler.result().iterator();
                            if (!iterator.hasNext()) {
                                monoSink.success();
                                return;
                            }
                            var row = iterator.next();
                            var endpointJson = row.getValue("endpoints");
                            var endpoints = endpointJson != null ? to(endpointJson) : new Endpoints();
                            monoSink.success(endpoints);
                        }));
    }

    public Flux<FacilityRepresentation> searchFacilityByName(String serviceName) {
        var searchQuery = "%" + serviceName.toUpperCase() + "%";
        return Flux.create(fluxSink -> this.readOnlyClient.preparedQuery(SELECT_FACILITIES_BY_NAME)
                .execute(Tuple.of(searchQuery),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                fluxSink.error(new DbOperationError("Failed to search facilities by name"));
                                return;
                            }

                            var rows = handler.result();

                            for (var row: rows) {
                                    fluxSink.next(toFacilityRepresentation(row));
                            }

                            fluxSink.complete();
                        }));
    }

    private FacilityRepresentation toFacilityRepresentation(Row row) {
        var facilityName = row.getString("name");
        var facilityId = row.getString("service_id");
        var isHIP = Boolean.TRUE.equals(row.getBoolean("is_hip"));
        var isHIU = Boolean.TRUE.equals(row.getBoolean("is_hiu"));
        var isLocker = Boolean.TRUE.equals(row.getBoolean("is_health_locker"));
        var serviceTypes = new ArrayList<ServiceType>();

        if(isHIP) serviceTypes.add(HIP);
        if(isHIU) serviceTypes.add(HIU);
        if(isLocker) serviceTypes.add(HEALTH_LOCKER);

        return  FacilityRepresentation.builder()
                .isHIP(isHIP)
                .identifier(new FacilityRepresentation.Identifier(facilityName, facilityId))
                .facilityType(serviceTypes)
                .build();
    }
}
