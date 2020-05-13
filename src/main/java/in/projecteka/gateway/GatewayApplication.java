package in.projecteka.gateway;

import in.projecteka.gateway.registry.YamlRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({YamlRegistry.class})
public class GatewayApplication {

	public static void main(String[] args) {
	    System.setProperty("spring.config.location","classpath:/registry.yaml,classpath:/application.yaml");
		SpringApplication.run(GatewayApplication.class, args);
	}

}
