package in.projecteka.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import in.projecteka.gateway.clients.DiscoveryServiceClient;
import in.projecteka.gateway.clients.LinkConfirmServiceClient;
import in.projecteka.gateway.clients.LinkInitServiceClient;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.common.cache.LoadingCacheAdapter;
import in.projecteka.gateway.common.cache.RedisCacheAdapter;
import in.projecteka.gateway.common.cache.RedisOptions;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.link.common.Validator;
import in.projecteka.gateway.link.common.RequestOrchestrator;
import in.projecteka.gateway.link.common.ResponseOrchestrator;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.YamlRegistry;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Configuration
public class GatewayConfiguration {
    @ConditionalOnProperty(value = "gateway.cacheMethod", havingValue = "redis")
    @Bean({"requestIdMappings"})
    public CacheAdapter<String,String> createRedisCacheAdapter(RedisOptions redisOptions) {
        RedisClient redisClient = getRedisClient(redisOptions);
        return new RedisCacheAdapter(redisClient, 5);
    }
    private RedisClient getRedisClient(RedisOptions redisOptions) {
        RedisURI redisUri = RedisURI.Builder.
                redis(redisOptions.getHost())
                .withPort(redisOptions.getPort())
                .withPassword(redisOptions.getPassword())
                .build();
        return RedisClient.create(redisUri);
    }
    @ConditionalOnProperty(value = "guava.cacheMethod", havingValue = "guava", matchIfMissing = true)
    @Bean({"requestIdMappings"})
    public CacheAdapter<String, String> createLoadingCacheAdapter() {
        return new LoadingCacheAdapter(createSessionCache(5));
    }

    public LoadingCache<String, String> createSessionCache(int duration) {
        return CacheBuilder
                .newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES)
                .build(new CacheLoader<String, String>() {
                    public String load(String key) {
                        return "";
                    }
                });
    }

    @Bean
    public YamlRegistry createYamlRegistry(ServiceOptions serviceOptions) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        return objectMapper.readValue(new File(serviceOptions.getRegistryPath()), YamlRegistry.class);
    }

    @Bean
    public CMRegistry cmRegistry(YamlRegistry yamlRegistry) {
        return new CMRegistry(yamlRegistry);
    }

    @Bean
    public BridgeRegistry bridgeRegistry(YamlRegistry yamlRegistry) {
        return new BridgeRegistry(yamlRegistry);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public DiscoveryServiceClient discoveryServiceClient(ServiceOptions serviceOptions,
                                                         WebClient.Builder builder,
                                                         ObjectMapper objectMapper) {
        return new DiscoveryServiceClient(serviceOptions,builder,objectMapper);
    }

    @Bean("discoveryRequestOrchestrator")
    public RequestOrchestrator<DiscoveryServiceClient> discoveryHelper(CacheAdapter<String, String> requestIdMappings,
                                                                       Validator validator,
                                                                       DiscoveryServiceClient discoveryServiceClient,
                                                                       CMRegistry cmRegistry) {
        return new RequestOrchestrator<>(requestIdMappings, validator, discoveryServiceClient, cmRegistry);
    }

    @Bean("discoveryResponseOrchestrator")
    public ResponseOrchestrator<DiscoveryServiceClient> discoveryResponseOrchestrator(Validator validator,
                                                                                      DiscoveryServiceClient discoveryServiceClient) {
        return new ResponseOrchestrator<>(validator, discoveryServiceClient);
    }

    @Bean
    public Validator validator(BridgeRegistry bridgeRegistry,
                               CMRegistry cmRegistry,
                               CacheAdapter<String,String> requestIdMappings) {
        return new Validator(bridgeRegistry, cmRegistry, requestIdMappings);
    }

    @Bean
    public LinkInitServiceClient linkInitServiceClient(ServiceOptions serviceOptions,
                                                       WebClient.Builder builder) {
        return new LinkInitServiceClient(builder,serviceOptions);
    }

    @Bean("linkInitRequestOrchestrator")
    public RequestOrchestrator<LinkInitServiceClient> linkInitRequestOrchestrator(CacheAdapter<String, String> requestIdMappings,
                                                                              Validator validator,
                                                                              LinkInitServiceClient linkInitServiceClient,
                                                                              CMRegistry cmRegistry) {
        return new RequestOrchestrator<>(requestIdMappings, validator, linkInitServiceClient, cmRegistry);
    }

    @Bean("linkInitResponseOrchestrator")
    public ResponseOrchestrator<LinkInitServiceClient> linkInitResponseOrchestrator(Validator validator,
                                                                                LinkInitServiceClient linkInitServiceClient) {
        return new ResponseOrchestrator<>(validator, linkInitServiceClient);
    }

    @Bean
    public LinkConfirmServiceClient linkConfirmServiceClient(ServiceOptions serviceOptions,
                                                                        WebClient.Builder builder) {
        return new LinkConfirmServiceClient(builder,serviceOptions);
    }

    @Bean("linkConfirmRequestOrchestrator")
    public RequestOrchestrator<LinkConfirmServiceClient> linkConfirmRequestOrchestrator(CacheAdapter<String, String> requestIdMappings,
                                                                              Validator validator,
                                                                              LinkConfirmServiceClient linkConfirmServiceClient,
                                                                              CMRegistry cmRegistry) {
        return new RequestOrchestrator<>(requestIdMappings, validator, linkConfirmServiceClient, cmRegistry);
    }

    @Bean("linkConfirmResponseOrchestrator")
    public ResponseOrchestrator<LinkConfirmServiceClient> linkConfirmResponseOrchestrator(Validator validator,
                                                                                LinkConfirmServiceClient linkConfirmServiceClient) {
        return new ResponseOrchestrator<>(validator, linkConfirmServiceClient);
    }

}
