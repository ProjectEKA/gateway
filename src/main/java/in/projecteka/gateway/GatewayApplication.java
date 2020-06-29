package in.projecteka.gateway;

import in.projecteka.gateway.clients.IdentityProperties;
import in.projecteka.gateway.common.cache.RedisOptions;
import in.projecteka.gateway.common.cache.ServiceOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({RedisOptions.class,
								ServiceOptions.class,
						IdentityProperties.class})
public class GatewayApplication {
	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}
}
