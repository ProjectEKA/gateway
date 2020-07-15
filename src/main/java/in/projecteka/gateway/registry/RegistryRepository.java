package in.projecteka.gateway.registry;

import in.projecteka.gateway.common.DbOperationError;
import in.projecteka.gateway.common.MappingRepository;
import in.projecteka.gateway.registry.Model.BridgeRegistryRequest;
import in.projecteka.gateway.registry.Model.BridgeServiceRequest;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class RegistryRepository {
    private static final Logger logger = LoggerFactory.getLogger(MappingRepository.class);
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
            "bridge_service (bridge_id, type, active, service_id) VALUES ($1, $2, $3, $4)";
    private static final String UPDATE_BRIDGE_SERVICE_ENTRY = "UPDATE bridge_service SET bridge_id = $1, active = $2, " +
            "date_modified = timezone('utc'::text, now()) " +
            "WHERE service_id = $3 AND type = $4";

    private final PgPool dbClient;

    public Mono<Boolean> ifPresent(String bridgeId) {
        return Mono.create(monoSink -> this.dbClient.preparedQuery(SELECT_BRIDGE_ID)
                .execute(Tuple.of(bridgeId),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to fetch bridge id"));
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
        return Mono.create(monoSink -> this.dbClient.preparedQuery(SELECT_ACTIVE_BRIDGE_SERVICE)
                .execute(Tuple.of(serviceId, type.toString(), active),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to fetch active bridge service"));
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

    public Mono<Boolean> ifPresent(String serviceId, ServiceType type) {
        return Mono.create(monoSink -> this.dbClient.preparedQuery(SELECT_BRIDGE_SERVICE)
                .execute(Tuple.of(serviceId, type.toString()),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to fetch bridge service"));
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

    public Mono<Void> insertBridgeServiceEntry(String bridgeId, BridgeServiceRequest request) {
        return Mono.create(monoSink -> dbClient.preparedQuery(INSERT_BRIDGE_SERVICE_ENTRY)
                .execute(Tuple.of(bridgeId, request.getType().toString(), request.isActive(),
                        request.getId()),
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
                .execute(Tuple.of(bridgeId, request.isActive(), request.getId(), request.getType().toString()),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to update bridge service entry"));
                                return;
                            }
                            monoSink.success();
                        }));
    }
}
