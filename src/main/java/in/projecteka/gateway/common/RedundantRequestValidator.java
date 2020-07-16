package in.projecteka.gateway.common;

import in.projecteka.gateway.clients.ClientError;
import in.projecteka.gateway.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

import static java.lang.String.format;
import static org.springframework.util.StringUtils.hasText;

@AllArgsConstructor
public class RedundantRequestValidator {
    private static final Logger logger = LoggerFactory.getLogger(RedundantRequestValidator.class);
    final CacheAdapter<String, String> requestIdTimestampMappings;
    final String keyPrefix;

    private String keyFor(String requestId) {
        return hasText(keyPrefix) ? format("%s_%s", keyPrefix, requestId) : requestId;
    }

    public Mono<Void> put(String requestId, String timestamp) {
        return requestIdTimestampMappings.put(keyFor(requestId), timestamp);
    }

    public Mono<Boolean> validate(String requestId, String timestamp) {
        return isRequestIdPresent(keyFor(requestId))
                .flatMap(result -> Mono.error(ClientError.tooManyRequests()))
                .then(Mono.just(isRequestIdValidInGivenTimestamp(timestamp)));
    }

    private Mono<Boolean> isRequestIdPresent(String requestId) {
        return requestIdTimestampMappings.get(requestId).map(StringUtils::hasText);
    }

    private boolean isRequestIdValidInGivenTimestamp(String timestamp) {
        try {
            return isValidTimestamp(toDate(timestamp));
        } catch (DateTimeParseException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    private LocalDateTime toDate(String timestamp) {
        DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .optionalStart()
                .appendOffsetId()
                .toFormatter();
        return LocalDateTime.parse(timestamp, dateTimeFormatter);
    }

    private boolean isValidTimestamp(LocalDateTime timestamp) {
        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime startTime = currentTime.minusMinutes(1);
        LocalDateTime endTime = currentTime.plusMinutes(9);
        return timestamp.isAfter(startTime) && timestamp.isBefore(endTime);
    }
}
