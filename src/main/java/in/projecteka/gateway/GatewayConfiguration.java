package in.projecteka.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import in.projecteka.gateway.clients.HipConsentNotifyServiceClient;
import in.projecteka.gateway.clients.ConsentRequestServiceClient;
import in.projecteka.gateway.clients.DiscoveryServiceClient;
import in.projecteka.gateway.clients.HiuConsentNotifyServiceClient;
import in.projecteka.gateway.clients.LinkConfirmServiceClient;
import in.projecteka.gateway.clients.LinkInitServiceClient;
import in.projecteka.gateway.clients.ClientRegistryClient;
import in.projecteka.gateway.clients.ClientRegistryProperties;
import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.common.cache.LoadingCacheAdapter;
import in.projecteka.gateway.common.cache.RedisCacheAdapter;
import in.projecteka.gateway.common.cache.RedisOptions;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.common.DefaultValidatedResponseAction;
import in.projecteka.gateway.common.RequestOrchestrator;
import in.projecteka.gateway.common.ResponseOrchestrator;
import in.projecteka.gateway.common.RetryableValidatedResponseAction;
import in.projecteka.gateway.common.Validator;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.YamlRegistry;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static in.projecteka.gateway.common.Constants.GW_LINK_QUEUE;

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

    @Bean("discoveryServiceClient")
    public DiscoveryServiceClient discoveryServiceClient(ServiceOptions serviceOptions,
                                                         WebClient.Builder builder,
                                                         CMRegistry cmRegistry,
                                                         CentralRegistry centralRegistry) {
        return new DiscoveryServiceClient(serviceOptions, builder, cmRegistry, centralRegistry);
    }

    @Bean("discoveryRequestOrchestrator")
    public RequestOrchestrator<DiscoveryServiceClient> discoveryHelper(CacheAdapter<String, String> requestIdMappings,
                                                                       Validator validator,
                                                                       DiscoveryServiceClient discoveryServiceClient) {
        return new RequestOrchestrator<>(requestIdMappings, validator, discoveryServiceClient);
    }

    @Bean("discoveryResponseAction")
    public DefaultValidatedResponseAction<DiscoveryServiceClient> discoveryResponseAction(DiscoveryServiceClient discoveryServiceClient,
                                                                                          CMRegistry cmRegistry) {
        return new DefaultValidatedResponseAction<>(discoveryServiceClient, cmRegistry);
    }

    @Bean("discoveryResponseOrchestrator")
    public ResponseOrchestrator discoveryResponseOrchestrator(Validator validator,
                                                              DefaultValidatedResponseAction<DiscoveryServiceClient> discoveryResponseAction) {
        return new ResponseOrchestrator(validator, discoveryResponseAction);
    }

    @Bean
    public Validator validator(BridgeRegistry bridgeRegistry,
                               CMRegistry cmRegistry,
                               CacheAdapter<String,String> requestIdMappings) {
        return new Validator(bridgeRegistry, cmRegistry, requestIdMappings);
    }

    @Bean("linkInitServiceClient")
    public LinkInitServiceClient linkInitServiceClient(ServiceOptions serviceOptions,
                                                       WebClient.Builder builder,
                                                       CMRegistry cmRegistry,
                                                       CentralRegistry centralRegistry) {
        return new LinkInitServiceClient(builder, serviceOptions, cmRegistry, centralRegistry);
    }

    @Bean("linkInitRequestOrchestrator")
    public RequestOrchestrator<LinkInitServiceClient> linkInitRequestOrchestrator(CacheAdapter<String, String> requestIdMappings,
                                                                              Validator validator,
                                                                              LinkInitServiceClient linkInitServiceClient) {
        return new RequestOrchestrator<>(requestIdMappings, validator, linkInitServiceClient);
    }

    @Bean("linkInitResponseAction")
    public DefaultValidatedResponseAction<LinkInitServiceClient> linkInitResponseAction(LinkInitServiceClient linkInitServiceClient,
                                                                                        CMRegistry cmRegistry) {
        return new DefaultValidatedResponseAction<>(linkInitServiceClient, cmRegistry);
    }

    @Bean("linkInitResponseOrchestrator")
    public ResponseOrchestrator linkInitResponseOrchestrator(Validator validator,
                                                         DefaultValidatedResponseAction<LinkInitServiceClient> linkInitResponseAction) {
        return new ResponseOrchestrator(validator, linkInitResponseAction);
    }

    @Bean
    public LinkConfirmServiceClient linkConfirmServiceClient(ServiceOptions serviceOptions,
                                                             WebClient.Builder builder,
                                                             CMRegistry cmRegistry,
                                                             CentralRegistry centralRegistry) {
        return new LinkConfirmServiceClient(builder,serviceOptions, cmRegistry, centralRegistry);
    }

    @Bean("linkConfirmRequestOrchestrator")
    public RequestOrchestrator<LinkConfirmServiceClient> linkConfirmRequestOrchestrator(CacheAdapter<String, String> requestIdMappings,
                                                                              Validator validator,
                                                                              LinkConfirmServiceClient linkConfirmServiceClient) {
        return new RequestOrchestrator<>(requestIdMappings, validator, linkConfirmServiceClient);
    }

    @Bean("linkConfirmResponseAction")
    public DefaultValidatedResponseAction<LinkConfirmServiceClient> linkConfirmResponseAction(LinkConfirmServiceClient linkConfirmServiceClient,
                                                                                              CMRegistry cmRegistry) {
        return new DefaultValidatedResponseAction<>(linkConfirmServiceClient, cmRegistry);
    }

    @Bean
    public Jackson2JsonMessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean("retryableLinkConfirmResponseAction")
    public RetryableValidatedResponseAction<LinkConfirmServiceClient> retryableLinkResponseAction(DefaultValidatedResponseAction<LinkConfirmServiceClient> linkConfirmResponseAction, AmqpTemplate amqpTemplate, Jackson2JsonMessageConverter converter, ServiceOptions serviceOptions) {
        return new RetryableValidatedResponseAction<>(amqpTemplate, converter, linkConfirmResponseAction, serviceOptions, GW_LINK_QUEUE);
    }

    @Bean
    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
                                             RetryableValidatedResponseAction<LinkConfirmServiceClient> retryableLinkResponseAction) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(GW_LINK_QUEUE);
        container.setMessageListener(retryableLinkResponseAction);
        return container;
    }


    @Bean("linkConfirmResponseOrchestrator")
    public ResponseOrchestrator linkConfirmResponseOrchestrator(Validator validator,
                                                         RetryableValidatedResponseAction<LinkConfirmServiceClient> retryableLinkConfirmResponseAction) {
        return new ResponseOrchestrator(validator, retryableLinkConfirmResponseAction);
    }

    @Bean
    public ConsentRequestServiceClient consentRequestServiceClient(ServiceOptions serviceOptions,
                                                                   WebClient.Builder builder,
                                                                   BridgeRegistry bridgeRegistry,
                                                                   CentralRegistry centralRegistry) {
        return new ConsentRequestServiceClient(serviceOptions, builder, bridgeRegistry, centralRegistry);
    }

    @Bean("consentRequestOrchestrator")
    public RequestOrchestrator<ConsentRequestServiceClient> consentRequestOrchestrator(CacheAdapter<String, String> requestIdMappings,
                                                                       Validator validator,
                                                                       ConsentRequestServiceClient consentRequestServiceClient) {
        return new RequestOrchestrator<>(requestIdMappings, validator, consentRequestServiceClient);
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

    @Bean
    public HipConsentNotifyServiceClient hipConsentNotifyServiceClient(ServiceOptions serviceOptions,
                                                                      WebClient.Builder builder,
                                                                      CentralRegistry centralRegistry,
                                                                      CMRegistry cmRegistry) {
        return new HipConsentNotifyServiceClient(serviceOptions, builder, centralRegistry, cmRegistry);
    }

    @Bean("hipConsentNotifyRequestOrchestrator")
    public RequestOrchestrator<HipConsentNotifyServiceClient> hipConsentNotifyRequestOrchestrator(
            CacheAdapter<String, String> requestIdMappings,
            Validator validator,
            HipConsentNotifyServiceClient hipConsentNotifyServiceClient) {
        return new RequestOrchestrator<>(requestIdMappings, validator, hipConsentNotifyServiceClient);
    }

    @Bean
    public HiuConsentNotifyServiceClient hiuConsentNotifyServiceClient(ServiceOptions serviceOptions,
                                                                       WebClient.Builder builder,
                                                                       CentralRegistry centralRegistry,
                                                                       CMRegistry cmRegistry) {
        return new HiuConsentNotifyServiceClient(serviceOptions, builder, centralRegistry, cmRegistry);
    }

    @Bean("hiuConsentNotifyRequestOrchestrator")
    public RequestOrchestrator<HiuConsentNotifyServiceClient> hiuConsentNotifyRequestOrchestrator(
            CacheAdapter<String, String> requestIdMappings,
            Validator validator,
            HiuConsentNotifyServiceClient hiuConsentNotifyServiceClient) {
        return new RequestOrchestrator<>(requestIdMappings, validator, hiuConsentNotifyServiceClient);
    }
}
