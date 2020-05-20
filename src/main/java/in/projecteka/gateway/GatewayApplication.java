package in.projecteka.gateway;

import in.projecteka.gateway.common.cache.RedisOptions;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.registry.YamlRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({YamlRegistry.class,
								RedisOptions.class,
								ServiceOptions.class})
public class GatewayApplication {

	public static void main(String[] args) {
	    System.setProperty("spring.config.location","classpath:/registry.yaml,classpath:/application.yaml");
		SpringApplication.run(GatewayApplication.class, args);
	}

}
