package in.projecteka.gateway.common.heartbeat;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import in.projecteka.gateway.clients.IdentityProperties;
import in.projecteka.gateway.common.heartbeat.model.HeartbeatResponse;
import in.projecteka.gateway.common.heartbeat.model.Status;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

import static in.projecteka.gateway.clients.model.Error.serviceDownError;

@AllArgsConstructor
public class Heartbeat {
    private RabbitmqOptions rabbitmqOptions;
    private IdentityProperties identityProperties;

    public Mono<HeartbeatResponse> getStatus() {
        try {
            return (isRedisUp() && isRabbitMQUp() && isKeycloakUp())
                    ? Mono.just(HeartbeatResponse.builder()
                    .timeStamp(Instant.now().toString())
                    .status(Status.UP)
                    .build())
                    : Mono.just(HeartbeatResponse.builder()
                    .timeStamp(Instant.now().toString())
                    .status(Status.DOWN)
                    .error(serviceDownError("Service Down"))
                    .build());
        } catch (IOException | InterruptedException | TimeoutException e) {
            return Mono.just(HeartbeatResponse.builder()
                    .timeStamp(Instant.now().toString())
                    .status(Status.DOWN)
                    .error(serviceDownError(e.getMessage()))
                    .build());
        }

    }

    private boolean isRabbitMQUp() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitmqOptions.getHost());
        factory.setPort(rabbitmqOptions.getPort());
        Connection connection = factory.newConnection();
        return connection.isOpen();
    }

    private boolean isRedisUp() throws IOException, InterruptedException {
        String command = "redis-cli ping";
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);
        int exitValue = process.waitFor();
        return exitValue == 0;
    }

    private boolean isKeycloakUp() throws IOException {
        URL siteUrl = new URL(identityProperties.getUrl());
        HttpURLConnection httpURLConnection = (HttpURLConnection) siteUrl.openConnection();
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.connect();
        int responseCode = httpURLConnection.getResponseCode();
        return responseCode == 200;
    }
}

