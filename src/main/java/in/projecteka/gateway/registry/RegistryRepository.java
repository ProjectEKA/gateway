package in.projecteka.gateway.registry;

import in.projecteka.gateway.common.DbOperationError;
import in.projecteka.gateway.registry.model.CMServiceRequest;
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
            "INSERT INTO consent_manager (id, name, url, cm_id, suffix, active, blocklisted, license, license_authority)"
                    + "VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)";
    private static final String UPDATE_CM_ENTRY =
            "UPDATE consent_manager SET name = $1, url = $2, active = $3, blocklisted = $4, license = $5," +
                    " license_authority = $6, date_modified = timezone('utc'::text, now()) " +
                    "WHERE consent_manager.suffix = $7";

    private final PgPool dbClient;

    public Mono<Integer> getCMEntryCount(CMServiceRequest request) {
        return Mono.create(monoSink -> dbClient.preparedQuery(GET_CM_ENTRY_COUNT)
                .execute(Tuple.of(request.getCmSuffix()), counter -> {
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
                .execute(Tuple.of(request.getId(), request.getName(), request.getUrl(),
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
}
