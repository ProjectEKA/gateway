package in.projecteka.gateway;

import in.projecteka.gateway.clients.ClientRegistryProperties;
import in.projecteka.gateway.common.cache.RedisOptions;
import in.projecteka.gateway.common.cache.ServiceOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({RedisOptions.class,
		ServiceOptions.class,
		ClientRegistryProperties.class})
public class GatewayApplication {

	public static void main(String[] args) {
	    System.setProperty("spring.config.location","classpath:/registry.yaml,classpath:/application.yaml");
		SpringApplication.run(GatewayApplication.class, args);
	}

}
