package in.projecteka.gateway.common.heartbeat;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import in.projecteka.gateway.clients.IdentityProperties;
import in.projecteka.gateway.common.cache.RedisOptions;
import in.projecteka.gateway.common.heartbeat.model.HeartbeatResponse;
import in.projecteka.gateway.common.heartbeat.model.Status;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.SocketAddress;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

import static in.projecteka.gateway.clients.model.Error.serviceDownError;

@AllArgsConstructor
public class Heartbeat {
    public static final String CACHE_METHOD_NAME = "guava";
    private final RabbitmqOptions rabbitmqOptions;
    private final IdentityProperties identityProperties;
    private final RedisOptions redisOptions;
    private final CacheMethodProperty cacheMethod;

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
        } catch (IOException | TimeoutException e) {
            return Mono.just(HeartbeatResponse.builder()
                    .timeStamp(Instant.now().toString())
                    .status(Status.DOWN)
                    .error(serviceDownError("Service Down"))
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

    private boolean isRedisUp() throws IOException {
        if (cacheMethod.getMethodName().equals(CACHE_METHOD_NAME))
            return true;
        return checkConnection(redisOptions.getHost(), redisOptions.getPort());
    }

    private boolean isKeycloakUp() throws IOException {
        URL siteUrl = new URL(identityProperties.getUrl());
        HttpURLConnection httpURLConnection = (HttpURLConnection) siteUrl.openConnection();
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.connect();
        int responseCode = httpURLConnection.getResponseCode();
        return responseCode == 200;
    }

    private boolean checkConnection(String host, int port) throws IOException {
        boolean isAlive;
        SocketAddress socketAddress = new InetSocketAddress(host, port);
        Socket socket = new Socket();
        socket.connect(socketAddress);
        isAlive = socket.isConnected();
        socket.close();
        return isAlive;
    }
}

