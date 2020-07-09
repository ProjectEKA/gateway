package in.projecteka.gateway.common;

import in.projecteka.gateway.registry.ServiceType;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class MappingRepository {
    private static final Logger logger = LoggerFactory.getLogger(MappingRepository.class);
    private static final String SELECT_CM_MAPPING = "select url from consent_manager" +
            " where cm_id = $1 and active = $2 and blocklisted = $3";
    private static final String SELECT_BRIDGE_MAPPING = "SELECT bridge.url FROM bridge " +
            "INNER JOIN bridge_service ON bridge_service.bridge_id = bridge.bridge_id " +
            "AND bridge_service.bridge_id = $1 AND bridge_service.type = $2 " +
            "WHERE bridge.active = $3 AND bridge.blocklisted = $4 AND bridge_service.active = $5";

    private final PgPool dbClient;

    public Mono<String> cmHost(String cmId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_CM_MAPPING)
                .execute(Tuple.of(cmId, true, false),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError());
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

    public Mono<String> bridgeHost(Pair<String, ServiceType> bridge) {
        return Mono.create(monoSink -> this.dbClient.preparedQuery(SELECT_BRIDGE_MAPPING)
                .execute(Tuple.of(bridge.getFirst(), bridge.getSecond().toString(), true, false, true),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError());
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

}
