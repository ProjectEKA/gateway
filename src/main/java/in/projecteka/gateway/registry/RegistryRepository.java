package in.projecteka.gateway.registry;

import in.projecteka.gateway.common.DbOperationError;
import in.projecteka.gateway.registry.model.CMServiceRequest;
import in.projecteka.gateway.registry.model.BridgeRegistryRequest;
import in.projecteka.gateway.registry.model.BridgeServiceRequest;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class RegistryRepository {
    private static final Logger logger = LoggerFactory.getLogger(RegistryRepository.class);
    private static final String GET_CM_ENTRY_COUNT =
            "SELECT COUNT(*) FROM consent_manager where suffix = $1";
    private static final String CREATE_CM_ENTRY =
            "INSERT INTO consent_manager (name, url, cm_id, suffix, active, blocklisted, license, licensing_authority)"
                    + "VALUES ($1, $2, $3, $4, $5, $6, $7, $8)";
    private static final String UPDATE_CM_ENTRY =
            "UPDATE consent_manager SET name = $1, url = $2, active = $3, blocklisted = $4, license = $5," +
                    " licensing_authority = $6, date_modified = timezone('utc'::text, now()) " +
                    "WHERE consent_manager.suffix = $7";

    private static final String SELECT_BRIDGE_ID = "SELECT bridge_id FROM bridge WHERE bridge_id = $1";
    private static final String INSERT_BRIDGE_ENTRY = "INSERT INTO " +
            "bridge (name, url, bridge_id, active, blocklisted) VALUES ($1, $2, $3, $4, $5)";
    private static final String UPDATE_BRIDGE_ENTRY = "UPDATE bridge SET name = $1, url = $2, active = $3, " +
            "blocklisted = $4, date_modified = timezone('utc'::text, now()) WHERE bridge.bridge_id = $5";

    private static final String SELECT_ACTIVE_BRIDGE_SERVICE = "SELECT * FROM bridge_service " +
            "WHERE service_id = $1 AND type = $2 AND active = $3";
    private static final String SELECT_BRIDGE_SERVICE = "SELECT * FROM bridge_service " +
            "WHERE service_id = $1 AND type = $2";
    private static final String INSERT_BRIDGE_SERVICE_ENTRY = "INSERT INTO " +
            "bridge_service (bridge_id, type, active, service_id, name) VALUES ($1, $2, $3, $4, $5)";
    private static final String UPDATE_BRIDGE_SERVICE_ENTRY = "UPDATE bridge_service SET bridge_id = $1, active = $2, " +
            "name = $3, date_modified = timezone('utc'::text, now()) FROM bridge " +
            "WHERE bridge_service.bridge_id = bridge.bridge_id AND bridge.active = $4 AND bridge_service.service_id = $5 AND bridge_service.type = $6";


    private final PgPool dbClient;

    public Mono<Integer> getCMEntryCount(String cmSuffix) {
        return Mono.create(monoSink -> dbClient.preparedQuery(GET_CM_ENTRY_COUNT)
                .execute(Tuple.of(cmSuffix), counter -> {
                    if (counter.failed()) {
                        logger.error(counter.cause().getMessage(), counter.cause());
                        monoSink.error(new DbOperationError("Failed to get CM entry"));
                        return;
                    }
                    Integer count = counter.result().iterator().next().getInteger("count");
                    monoSink.success(count);
                }));
    }

    public Mono<Void> createCMEntry(CMServiceRequest request) {
        return Mono.create(monoSink -> dbClient.preparedQuery(CREATE_CM_ENTRY)
                .execute(Tuple.of(request.getName(), request.getUrl(),
                        request.getConsentManagerId(), request.getCmSuffix(), request.getIsActive(),
                        request.getIsBlocklisted(), request.getLicense(), request.getLicenseAuthority()),
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
        return Mono.create(monoSink -> dbClient.preparedQuery(UPDATE_CM_ENTRY)
                .execute(Tuple.of(request.getName(), request.getUrl(), request.getIsActive(),
                        request.getIsBlocklisted(), request.getLicense(), request.getLicenseAuthority()
                        , request.getCmSuffix()),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to update CM entry"));
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<Boolean> ifPresent(String bridgeId) {
        return select(SELECT_BRIDGE_ID, Tuple.of(bridgeId), "Failed to fetch bridge id");
    }

    public Mono<Void> insertBridgeEntry(BridgeRegistryRequest request) {
        return Mono.create(monoSink -> dbClient.preparedQuery(INSERT_BRIDGE_ENTRY)
                .execute(Tuple.of(request.getName(), request.getUrl(), request.getId(),
                        request.isActive(), request.isBlocklisted()),
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
        return Mono.create(monoSink -> dbClient.preparedQuery(UPDATE_BRIDGE_ENTRY)
                .execute(Tuple.of(request.getName(), request.getUrl(),
                        request.isActive(), request.isBlocklisted(), request.getId()),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to update bridge entry"));
                                return;
                            }
                            monoSink.success();
                        }));
    }
    
    public Mono<Boolean> ifPresent(String serviceId, ServiceType type, boolean active) {
        return select(SELECT_ACTIVE_BRIDGE_SERVICE,
                Tuple.of(serviceId, type.toString(), active),
                "Failed to fetch active bridge service");
    }

    public Mono<Boolean> ifPresent(String serviceId, ServiceType type) {
        return select(SELECT_BRIDGE_SERVICE,
                Tuple.of(serviceId, type.toString()),
                "Failed to fetch bridge service");
    }

    public Mono<Void> insertBridgeServiceEntry(String bridgeId, BridgeServiceRequest request) {
        return Mono.create(monoSink -> dbClient.preparedQuery(INSERT_BRIDGE_SERVICE_ENTRY)
                .execute(Tuple.of(bridgeId, request.getType().toString(), request.isActive(),
                        request.getId(), request.getName()),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to insert bridge service entry"));
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<Void> updateBridgeServiceEntry(String bridgeId, BridgeServiceRequest request) {
        return Mono.create(monoSink -> dbClient.preparedQuery(UPDATE_BRIDGE_SERVICE_ENTRY)
                .execute(Tuple.of(bridgeId, request.isActive(), request.getName(), true,
                        request.getId(), request.getType().toString()),
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
        return Mono.create(monoSink -> this.dbClient.preparedQuery(query)
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
}
