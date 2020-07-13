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
    private static final String UPSERT_BRIDGE_ENTRY = "INSERT INTO " +
            "bridge (name, url, bridge_id, active, blocklisted) VALUES ($1, $2, $3, $4, $5) " +
            "ON CONFLICT (bridge_id) DO " +
            "UPDATE SET name = $1, url = $2, active = $4, blocklisted = $5, " +
            "date_modified = timezone('utc'::text, now()) WHERE bridge.bridge_id = $3";

    private static final String UPSERT_BRIDGE_SERVICE_ENTRIES = "INSERT INTO " +
            "bridge_service (bridge_id, type, active, service_id) VALUES ($1, $2, $3, $4) " +
            "ON CONFLICT ON CONSTRAINT uk_bridge_service_bridge_id_type DO " +
            "UPDATE SET name = $1, url = $2, active = $4, blocklisted = $5, " +
            "date_modified = timezone('utc'::text, now()) WHERE bridge.bridge_id = $3";

    private final PgPool dbClient;

    public Mono<Void> upsertBridgeEntry(BridgeRegistryRequest request) {
        return Mono.create(monoSink -> this.dbClient.preparedQuery(UPSERT_BRIDGE_ENTRY)
                .execute(Tuple.of(request.getName(), request.getUrl(), request.getId(),
                        request.isActive(), request.isBlocklisted()),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to upsert bridge entry"));
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<Void> upsertBridgeServiceEntries(BridgeServiceRequest request) {
//        return Mono.create(monoSink -> this.dbClient.preparedQuery(UPSERT_BRIDGE_ENTRY)
//                .execute(Tuple.of(request.getName(), request.getUrl(), request.getId(),
//                        request.isActive(), request.isBlocklisted()),
//                        handler -> {
//                            if (handler.failed()) {
//                                logger.error(handler.cause().getMessage(), handler.cause());
//                                monoSink.error(new DbOperationError("Failed to upsert bridge entry"));
//                                return;
//                            }
//                            monoSink.success();
//                        }));
        return Mono.empty();
    }
}
