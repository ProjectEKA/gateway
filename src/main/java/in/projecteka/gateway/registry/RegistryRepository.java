package in.projecteka.gateway.registry;

import in.projecteka.gateway.common.DbOperationError;
import in.projecteka.gateway.common.MappingRepository;
import in.projecteka.gateway.registry.Model.CMServiceRequest;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class RegistryRepository {
    private static final Logger logger = LoggerFactory.getLogger(MappingRepository.class);
    private static final String UPSERT_CM_ENTRY =
            "INSERT INTO consent_manager (id, name, url, cm_id, suffix, active, blocklisted, license, license_authority)" +
                    "VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9) ON CONFLICT (cm_id) DO" +
                    "UPDATE SET name = $2, url = $3, active = $6, blocklisted = $7, date_modified = timezone('utc'::text, now())" +
                    "WHERE consent_manager.cm_id = $4";

    private final PgPool dbClient;

    public Mono<Void> upsertCMEntry(CMServiceRequest request) {
        return Mono.create(monoSink -> this.dbClient.preparedQuery(UPSERT_CM_ENTRY)
                .execute(Tuple.of(request.getId(), request.getName(), request.getUrl(),
                        request.getConsentManagerId(), request.getCmSuffix(), request.getIsActive(),
                        request.getIsBlocklisted(), request.getLicense(), request.getLicenseAuthority()),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to upsert CM entry"));
                                return;
                            }
                            monoSink.success();
                        }));
    }
}
