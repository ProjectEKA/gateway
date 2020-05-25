package in.projecteka.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import in.projecteka.gateway.clients.DiscoveryServiceClient;
import in.projecteka.gateway.clients.LinkServiceClient;
import in.projecteka.gateway.clients.ClientRegistryClient;
import in.projecteka.gateway.clients.ClientRegistryProperties;
import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.common.cache.LoadingCacheAdapter;
import in.projecteka.gateway.common.cache.RedisCacheAdapter;
import in.projecteka.gateway.common.cache.RedisOptions;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.link.discovery.DiscoveryHelper;
import in.projecteka.gateway.link.discovery.DiscoveryValidator;
import in.projecteka.gateway.link.link.LinkHelper;
import in.projecteka.gateway.link.link.LinkValidator;
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
                                                         ObjectMapper objectMapper,
                                                         CentralRegistry centralRegistry) {
        return new DiscoveryServiceClient(serviceOptions,builder,objectMapper, centralRegistry);
    }

    @Bean
    public DiscoveryValidator discoveryValidator(BridgeRegistry bridgeRegistry,
                                                 CMRegistry cmRegistry,
                                                 DiscoveryServiceClient discoveryServiceClient) {
        return new DiscoveryValidator(bridgeRegistry, cmRegistry, discoveryServiceClient);
    }

    @Bean
    public DiscoveryHelper discoveryHelper(CacheAdapter<String, String> requestIdMappings,
                                           DiscoveryValidator discoveryValidator,
                                           DiscoveryServiceClient discoveryServiceClient) {
        return new DiscoveryHelper(requestIdMappings, discoveryValidator, discoveryServiceClient);
    }

    @Bean
    public LinkValidator linkValidator(BridgeRegistry bridgeRegistry,
                                       CMRegistry cmRegistry,
                                       LinkServiceClient linkServiceClient,
                                       CacheAdapter<String,String> requestIdMappings) {
        return new LinkValidator(bridgeRegistry, cmRegistry, linkServiceClient, requestIdMappings);
    }

    @Bean
    public LinkServiceClient linkServiceClient(ServiceOptions serviceOptions,
                                               WebClient.Builder builder) {
        return new LinkServiceClient(builder,serviceOptions);
    }

    @Bean
    public LinkHelper linkHelper(LinkValidator linkValidator,
                                 CacheAdapter<String,String> requestIdMappings,
                                 LinkServiceClient linkServiceClient) {
        return new LinkHelper(linkValidator, requestIdMappings, linkServiceClient);
    }
    @Bean
    public ClientRegistryClient clientRegistryClient(WebClient.Builder builder,
                                                     ClientRegistryProperties clientRegistryProperties) {
        return new ClientRegistryClient(builder, clientRegistryProperties.getUrl());
    }

    @Bean
    public CentralRegistry centralRegistry(ClientRegistryProperties clientRegistryProperties,
                                           ClientRegistryClient clientRegistryClient) {
        return new CentralRegistry(clientRegistryClient, clientRegistryProperties);
    }
}
