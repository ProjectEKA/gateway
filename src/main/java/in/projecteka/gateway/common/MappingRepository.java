package in.projecteka.gateway.common;

import in.projecteka.gateway.registry.ServiceType;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class MappingRepository {
    private static final Logger logger = LoggerFactory.getLogger(MappingRepository.class);
    private static final String SELECT_CM_MAPPING = "select url from consent_manager" +
            " where cm_id = $1 and active = $2 and blocklisted = $3";
    private static final String SELECT_BRIDGE_MAPPING = "SELECT bridge.url FROM bridge " +
            "INNER JOIN bridge_service ON bridge_service.bridge_id = bridge.bridge_id " +
            "AND bridge_service.service_id = $1 AND bridge_service.type = $2 " +
            "WHERE bridge.active = $3 AND bridge.blocklisted = $4 AND bridge_service.active = $5";
    private static final String SELECT_BRIDGEURLS = "select url from bridge";

    private final PgPool dbClient;

    public Mono<String> cmHost(String cmId) {
        return select(SELECT_CM_MAPPING, Tuple.of(cmId, true, false), "Failed to fetch CM host");
    }

    public Mono<String> bridgeHost(Pair<String, ServiceType> bridge) {
        return select(SELECT_BRIDGE_MAPPING,
                Tuple.of(bridge.getFirst(), bridge.getSecond().toString(), true, false, true),
                "Failed to fetch Bridge host");
    }

    private Mono<String> select(String query, Tuple params, String errorMessage) {
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
                                monoSink.success();
                                return;
                            }
                            monoSink.success(iterator.next().getString(0));
                        }));
    }

    public Flux<String> selectBridgeUrls() {
        return Flux.create(fluxSink -> dbClient.preparedQuery(SELECT_BRIDGEURLS)
                .execute(
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                fluxSink.error(new DbOperationError("failed to get Urls"));
                            } else {
                                RowSet<Row> results = handler.result();
                                for (Row row : results) {
                                    fluxSink.next(row.getString(0));
                                }
                            }
                            fluxSink.complete();
                        }
                ));
    }
}
