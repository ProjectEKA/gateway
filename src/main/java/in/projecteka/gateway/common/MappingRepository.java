package in.projecteka.gateway.common;

import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class MappingRepository {
    private static final Logger logger = LoggerFactory.getLogger(MappingRepository.class);
    private static final String SELECT_CM_MAPPING = "select url from consent_manager where cm_id = $1 and active = $2 and blacklisted = $3";
    private static final String SELECT_BRIDGE_MAPPING = "";

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
                            var host = iterator.next().getString("url");
                            monoSink.success(host);
                        }));
    }

    public Mono<String> bridgeHost(String bridgeId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_BRIDGE_MAPPING)
                .execute(Tuple.of(bridgeId, true, false, true),
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
                            var host = iterator.next().getString("url");
                            monoSink.success(host);
                        }));
    }

}
