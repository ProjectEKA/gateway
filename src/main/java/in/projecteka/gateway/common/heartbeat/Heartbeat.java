package in.projecteka.gateway.common.heartbeat;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import in.projecteka.gateway.clients.IdentityProperties;
import in.projecteka.gateway.common.heartbeat.model.HeartbeatResponse;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

import static in.projecteka.gateway.clients.model.Error.of;
import static in.projecteka.gateway.common.heartbeat.model.Status.DOWN;
import static in.projecteka.gateway.common.heartbeat.model.Status.UP;
import static java.lang.String.valueOf;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.time.LocalDateTime.now;
import static java.time.ZoneOffset.UTC;
import static org.springframework.http.HttpMethod.GET;
import static reactor.core.publisher.Mono.just;

@AllArgsConstructor
public class Heartbeat {
    public static final String SERVICE_DOWN = "Service Down";
    private final RabbitmqOptions rabbitmqOptions;
    private final IdentityProperties identityProperties;
    private final CacheHealth cacheHealth;

    public Mono<HeartbeatResponse> getStatus() {
        try {
            return (cacheHealth.isUp() && isRabbitMQUp() && isKeycloakUp())
                   ? just(HeartbeatResponse.builder().timeStamp(now(UTC)).status(UP).build())
                   : just(HeartbeatResponse.builder().timeStamp(now(UTC)).status(DOWN).error(of(SERVICE_DOWN)).build());
        } catch (IOException | TimeoutException | KeyManagementException | NoSuchAlgorithmException e) {
            return just(HeartbeatResponse.builder().timeStamp(now(UTC)).status(DOWN).error(of(SERVICE_DOWN)).build());
        }
    }

    private boolean isRabbitMQUp() throws IOException, TimeoutException, KeyManagementException, NoSuchAlgorithmException {
        var factory = new ConnectionFactory();
        factory.setHost(rabbitmqOptions.getHost());
        factory.setPort(rabbitmqOptions.getPort());
        factory.setUsername(rabbitmqOptions.getUsername());
        factory.setPassword(rabbitmqOptions.getPassword());
        if(rabbitmqOptions.isUseSSL()){
            factory.useSslProtocol();
        }
        try (Connection connection = factory.newConnection()) {
            return connection.isOpen();
        }
    }

    private boolean isKeycloakUp() throws IOException {
        var siteUrl = new URL(identityProperties.getUrl());
        HttpURLConnection httpURLConnection = (HttpURLConnection) siteUrl.openConnection();
        httpURLConnection.setRequestMethod(valueOf(GET));
        httpURLConnection.connect();
        return httpURLConnection.getResponseCode() == HTTP_OK;
    }
}

