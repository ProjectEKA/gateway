package in.projecteka.gateway;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import in.projecteka.gateway.clients.AdminServiceClient;
import in.projecteka.gateway.clients.AuthConfirmServiceClient;
import in.projecteka.gateway.clients.AuthModeFetchClient;
import in.projecteka.gateway.clients.AuthNotifyServiceClient;
import in.projecteka.gateway.clients.ConsentFetchServiceClient;
import in.projecteka.gateway.clients.ConsentRequestServiceClient;
import in.projecteka.gateway.clients.ConsentStatusServiceClient;
import in.projecteka.gateway.clients.DataFlowRequestServiceClient;
import in.projecteka.gateway.clients.DiscoveryServiceClient;
import in.projecteka.gateway.clients.FacilityRegistryClient;
import in.projecteka.gateway.clients.GlobalExceptionHandler;
import in.projecteka.gateway.clients.HealthInfoNotificationServiceClient;
import in.projecteka.gateway.clients.HipConsentNotifyServiceClient;
import in.projecteka.gateway.clients.HipDataFlowServiceClient;
import in.projecteka.gateway.clients.HipDataNotificationServiceClient;
import in.projecteka.gateway.clients.HipInitLinkServiceClient;
import in.projecteka.gateway.clients.HiuConsentNotifyServiceClient;
import in.projecteka.gateway.clients.HiuSubscriptionNotifyServiceClient;
import in.projecteka.gateway.clients.IdentityProperties;
import in.projecteka.gateway.clients.IdentityServiceClient;
import in.projecteka.gateway.clients.LinkConfirmServiceClient;
import in.projecteka.gateway.clients.LinkInitServiceClient;
import in.projecteka.gateway.clients.PatientSMSNotificationClient;
import in.projecteka.gateway.clients.PatientSearchServiceClient;
import in.projecteka.gateway.clients.PatientServiceClient;
import in.projecteka.gateway.clients.SubscriptionRequestNotifyServiceClient;
import in.projecteka.gateway.clients.SubscriptionRequestServiceClient;
import in.projecteka.gateway.clients.UserAuthenticatorClient;
import in.projecteka.gateway.common.DefaultValidatedRequestAction;
import in.projecteka.gateway.common.DefaultValidatedResponseAction;
import in.projecteka.gateway.common.IdentityService;
import in.projecteka.gateway.common.MappingRepository;
import in.projecteka.gateway.common.MappingService;
import in.projecteka.gateway.common.RedundantRequestValidator;
import in.projecteka.gateway.common.RequestOrchestrator;
import in.projecteka.gateway.common.ResponseOrchestrator;
import in.projecteka.gateway.common.RetryableValidatedRequestAction;
import in.projecteka.gateway.common.RetryableValidatedResponseAction;
import in.projecteka.gateway.common.Validator;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.common.cache.LoadingCacheAdapter;
import in.projecteka.gateway.common.cache.RedisCacheAdapter;
import in.projecteka.gateway.common.cache.RedisOptions;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.common.heartbeat.CacheHealth;
import in.projecteka.gateway.common.heartbeat.CacheMethodProperty;
import in.projecteka.gateway.common.heartbeat.Heartbeat;
import in.projecteka.gateway.common.heartbeat.RabbitmqOptions;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.FacilityRegistryProperties;
import in.projecteka.gateway.registry.RegistryRepository;
import in.projecteka.gateway.registry.RegistryService;
import in.projecteka.gateway.registry.ServiceType;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.rabbitmq.ChannelPoolFactory;
import reactor.rabbitmq.ChannelPoolOptions;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.ReceiverOptions;
import reactor.rabbitmq.SenderOptions;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static in.projecteka.gateway.common.Constants.GW_DATAFLOW_QUEUE;
import static in.projecteka.gateway.common.Constants.GW_LINK_QUEUE;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static reactor.rabbitmq.Utils.singleConnectionMono;

@Configuration
public class GatewayConfiguration {
    @Value("${webclient.maxInMemorySize}")
    private int maxInMemorySize;

    @ConditionalOnProperty(value = "gateway.cacheMethod", havingValue = "guava", matchIfMissing = true)
    @Bean("accessToken")
    public CacheAdapter<String, String> createLoadingCacheAdapterForAccessToken() {
        return new LoadingCacheAdapter<>(stringStringLoadingCache(5));
    }

    public LoadingCache<String, String> stringStringLoadingCache(int duration) {
        return CacheBuilder
                .newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public String load(String key) {
                        return "";
                    }
                });
    }

    @ConditionalOnProperty(value = "gateway.cacheMethod", havingValue = "redis")
    @Bean("accessToken")
    public CacheAdapter<String, String> createRedisCacheAdapterForAccessToken(
            @Qualifier("Lettuce") RedisClient redisClient,
            RedisOptions redisOptions,
            IdentityProperties identityProperties) {
        return new RedisCacheAdapter(redisClient,
                identityProperties.getAccessTokenExpiryInMinutes(),
                redisOptions.getRetry());
    }

    @ConditionalOnProperty(value = "gateway.cacheMethod", havingValue = "guava", matchIfMissing = true)
    @Bean("facilityTokenCache")
    public CacheAdapter<String, String> createLoadingCacheAdapterForFacilityAccessToken() {
        return new LoadingCacheAdapter<>(stringStringLoadingCache(5));
    }

    @ConditionalOnProperty(value = "gateway.cacheMethod", havingValue = "redis")
    @Bean("facilityTokenCache")
    public CacheAdapter<String, String> createRedisCacheAdapterForFacilityAccessToken(
            @Qualifier("Lettuce") RedisClient redisClient,
            RedisOptions redisOptions,
            FacilityRegistryProperties facilityRegistryProperties) {
        return new RedisCacheAdapter(redisClient,
                facilityRegistryProperties.getTokenExpiry(),
                redisOptions.getRetry());
    }

    @ConditionalOnProperty(value = "gateway.cacheMethod", havingValue = "redis")
    @Bean({"requestIdMappings", "requestIdTimestampMappings"})
    public CacheAdapter<String, String> createRedisCacheAdapter(@Qualifier("Lettuce") RedisClient redisClient,
                                                                RedisOptions redisOptions) {
        return new RedisCacheAdapter(redisClient, redisOptions.getExpiry(), redisOptions.getRetry());
    }

    @ConditionalOnProperty(value = "gateway.cacheMethod", havingValue = "redis")
    @Bean("Lettuce")
    RedisClient redis(RedisOptions redisOptions) {
        var socketOptions = SocketOptions.builder().keepAlive(redisOptions.isKeepAliveEnabled()).build();
        ClientOptions clientOptions = ClientOptions.builder().socketOptions(socketOptions).build();
        RedisURI redisUri = RedisURI.Builder.
                redis(redisOptions.getHost())
                .withPort(redisOptions.getPort())
                .withPassword(redisOptions.getPassword())
                .build();
        RedisClient redisClient = RedisClient.create(redisUri);
        redisClient.setOptions(clientOptions);
        return redisClient;
    }

    @ConditionalOnProperty(value = "gateway.cacheMethod", havingValue = "guava", matchIfMissing = true)
    @Bean("Lettuce")
    RedisClient dummyRedis() {
        var redisUri = RedisURI.Builder.redis("localhost").build();
        return RedisClient.create(redisUri);
    }

    @Bean
    CacheHealth cacheHealth(CacheMethodProperty cacheMethodProperty, @Qualifier("Lettuce") RedisClient redisClient) {
        return new CacheHealth(cacheMethodProperty, redisClient);
    }

    @ConditionalOnProperty(value = "guava.cacheMethod", havingValue = "guava", matchIfMissing = true)
    @Bean({"requestIdMappings", "requestIdTimestampMappings"})
    public CacheAdapter<String, String> createLoadingCacheAdapter() {
        return new LoadingCacheAdapter<>(createSessionCache(10));
    }

    public LoadingCache<String, String> createSessionCache(int duration) {
        return CacheBuilder
                .newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public String load(String key) {
                        return "";
                    }
                });
    }

    @ConditionalOnProperty(value = "gateway.cacheMethod", havingValue = "redis")
    @Bean("consentManagerMappings")
    public CacheAdapter<String, String> createRedisCacheAdapterForCMMappings(
            @Qualifier("Lettuce") RedisClient redisClient,
            RedisOptions redisOptions) {
        int _12Hours = 12 * 60;
        return new RedisCacheAdapter(redisClient, _12Hours, redisOptions.getRetry());
    }

    @ConditionalOnProperty(value = "guava.cacheMethod", havingValue = "guava", matchIfMissing = true)
    @Bean({"consentManagerMappings"})
    public CacheAdapter<String, String> createLoadingCacheAdapterForCMMappings() {
        return new LoadingCacheAdapter<>(stringStringLoadingCache(12));
    }

    @ConditionalOnProperty(value = "gateway.cacheMethod", havingValue = "redis")
    @Bean("bridgeMappings")
    public CacheAdapter<String, String> createRedisCacheAdapterForBridgeMappings(
            @Qualifier("Lettuce") RedisClient redisClient,
            RedisOptions redisOptions,
            @Value("${gateway.bridgeCacheExpiry}") int expiry) {
        return new RedisCacheAdapter(redisClient, expiry, redisOptions.getRetry());
    }

    @ConditionalOnProperty(value = "guava.cacheMethod", havingValue = "guava", matchIfMissing = true)
    @Bean({"bridgeMappings"})
    public CacheAdapter<String, String> createLoadingCacheAdapterForBridgeMappings(
            @Value("${gateway.bridgeCacheExpiry}") int expiry) {
        return new LoadingCacheAdapter<>(stringStringLoadingCache(expiry));
    }

    @Bean
    public CMRegistry cmRegistry(CacheAdapter<String, String> consentManagerMappings, MappingRepository mappingRepository) {
        return new CMRegistry(consentManagerMappings, mappingRepository);
    }

    @Bean
    public BridgeRegistry bridgeRegistry(CacheAdapter<String, String> bridgeMappings,
                                         MappingRepository mappingRepository) {
        return new BridgeRegistry(bridgeMappings, mappingRepository);
    }

    @Bean
    public MappingService mappingService(MappingRepository mappingRepository) {
        return new MappingService(mappingRepository);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Bean("discoveryServiceClient")
    public DiscoveryServiceClient discoveryServiceClient(ServiceOptions serviceOptions,
                                                         @Qualifier("customBuilder") WebClient.Builder builder,
                                                         CMRegistry cmRegistry,
                                                         IdentityService identityService,
                                                         BridgeRegistry bridgeRegistry) {
        return new DiscoveryServiceClient(serviceOptions, builder, identityService, cmRegistry, bridgeRegistry);
    }

    @Bean("discoveryRequestAction")
    public DefaultValidatedRequestAction<DiscoveryServiceClient> discoveryRequestAction(
            DiscoveryServiceClient discoveryServiceClient) {
        return new DefaultValidatedRequestAction<>(discoveryServiceClient);
    }

    @Bean("discoveryRequestOrchestrator")
    public RequestOrchestrator<DiscoveryServiceClient> discoveryHelper(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            DiscoveryServiceClient discoveryServiceClient,
            DefaultValidatedRequestAction<DiscoveryServiceClient> discoveryRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                discoveryServiceClient,
                discoveryRequestAction);
    }

    @Bean("discoveryResponseAction")
    public DefaultValidatedResponseAction<DiscoveryServiceClient> discoveryResponseAction(
            DiscoveryServiceClient discoveryServiceClient) {
        return new DefaultValidatedResponseAction<>(discoveryServiceClient);
    }

    @Bean("discoveryResponseOrchestrator")
    public ResponseOrchestrator discoveryResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<DiscoveryServiceClient> discoveryResponseAction) {
        return new ResponseOrchestrator(validator, discoveryResponseAction);
    }

    @Bean
    public Validator validator(BridgeRegistry bridgeRegistry,
                               CMRegistry cmRegistry,
                               @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
                               RedundantRequestValidator redundantRequestValidator) {
        return new Validator(bridgeRegistry, cmRegistry, requestIdMappings, redundantRequestValidator);
    }

    @Bean("linkInitServiceClient")
    public LinkInitServiceClient linkInitServiceClient(ServiceOptions serviceOptions,
                                                       @Qualifier("customBuilder") WebClient.Builder builder,
                                                       CMRegistry cmRegistry,
                                                       IdentityService identityService,
                                                       BridgeRegistry bridgeRegistry) {
        return new LinkInitServiceClient(builder, serviceOptions, identityService, cmRegistry, bridgeRegistry);
    }

    @Bean("linkInitRequestAction")
    public DefaultValidatedRequestAction<LinkInitServiceClient> linkInitRequestAction(
            LinkInitServiceClient linkInitServiceClient) {
        return new DefaultValidatedRequestAction<>(linkInitServiceClient);
    }

    @Bean("linkInitRequestOrchestrator")
    public RequestOrchestrator<LinkInitServiceClient> linkInitRequestOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            LinkInitServiceClient linkInitServiceClient,
            DefaultValidatedRequestAction<LinkInitServiceClient> linkInitRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                linkInitServiceClient,
                linkInitRequestAction);
    }

    @Bean("linkInitResponseAction")
    public DefaultValidatedResponseAction<LinkInitServiceClient> linkInitResponseAction(
            LinkInitServiceClient linkInitServiceClient) {
        return new DefaultValidatedResponseAction<>(linkInitServiceClient);
    }

    @Bean("linkInitResponseOrchestrator")
    public ResponseOrchestrator linkInitResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<LinkInitServiceClient> linkInitResponseAction) {
        return new ResponseOrchestrator(validator, linkInitResponseAction);
    }

    @Bean
    public LinkConfirmServiceClient linkConfirmServiceClient(
            ServiceOptions serviceOptions,
            @Qualifier("customBuilder") WebClient.Builder builder,
            CMRegistry cmRegistry,
            IdentityService identityService,
            BridgeRegistry bridgeRegistry) {
        return new LinkConfirmServiceClient(builder, serviceOptions, identityService, cmRegistry, bridgeRegistry);
    }

    @Bean("linkConfirmRequestAction")
    public DefaultValidatedRequestAction<LinkConfirmServiceClient> linkConfirmRequestAction(
            LinkConfirmServiceClient linkConfirmServiceClient) {
        return new DefaultValidatedRequestAction<>(linkConfirmServiceClient);
    }

    @Bean("linkConfirmRequestOrchestrator")
    public RequestOrchestrator<LinkConfirmServiceClient> linkConfirmRequestOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            LinkConfirmServiceClient linkConfirmServiceClient,
            DefaultValidatedRequestAction<LinkConfirmServiceClient> linkConfirmRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                linkConfirmServiceClient,
                linkConfirmRequestAction);
    }

    @Bean("linkConfirmResponseAction")
    public DefaultValidatedResponseAction<LinkConfirmServiceClient> linkConfirmResponseAction(
            LinkConfirmServiceClient linkConfirmServiceClient) {
        return new DefaultValidatedResponseAction<>(linkConfirmServiceClient);
    }

    @Bean("retryableLinkConfirmResponseAction")
    public RetryableValidatedResponseAction<LinkConfirmServiceClient> retryableLinkResponseAction(
            DefaultValidatedResponseAction<LinkConfirmServiceClient> linkConfirmResponseAction,
            ReceiverOptions receiverOptions,
            SenderOptions senderOptions,
            ServiceOptions serviceOptions) {
        return new RetryableValidatedResponseAction<>(
                RabbitFlux.createReceiver(receiverOptions),
                RabbitFlux.createSender(senderOptions),
                linkConfirmResponseAction,
                serviceOptions,
                GW_LINK_QUEUE,
                X_CM_ID);
    }


    @Bean("linkConfirmResponseOrchestrator")
    public ResponseOrchestrator linkConfirmResponseOrchestrator(
            Validator validator,
            RetryableValidatedResponseAction<LinkConfirmServiceClient> retryableLinkConfirmResponseAction) {
        return new ResponseOrchestrator(validator, retryableLinkConfirmResponseAction);
    }

    @Bean
    public ConsentRequestServiceClient consentRequestServiceClient(
            ServiceOptions serviceOptions,
            @Qualifier("customBuilder") WebClient.Builder builder,
            BridgeRegistry bridgeRegistry,
            IdentityService identityService,
            CMRegistry cmRegistry) {
        return new ConsentRequestServiceClient(serviceOptions, builder, identityService, bridgeRegistry, cmRegistry);
    }

    @Bean
    public ConsentFetchServiceClient consentFetchServiceClient(
            ServiceOptions serviceOptions,
            @Qualifier("customBuilder") WebClient.Builder builder,
            BridgeRegistry bridgeRegistry,
            IdentityService identityService,
            CMRegistry cmRegistry) {
        return new ConsentFetchServiceClient(serviceOptions, builder, identityService, bridgeRegistry, cmRegistry);
    }

    @Bean
    public ConsentStatusServiceClient consentStatusServiceClient(
            ServiceOptions serviceOptions,
            @Qualifier("customBuilder") WebClient.Builder builder,
            BridgeRegistry bridgeRegistry,
            IdentityService identityService,
            CMRegistry cmRegistry) {
        return new ConsentStatusServiceClient(serviceOptions, builder, identityService, cmRegistry, bridgeRegistry);
    }

    @Bean("consentRequestAction")
    public DefaultValidatedRequestAction<ConsentRequestServiceClient> consentRequestAction(
            ConsentRequestServiceClient consentRequestServiceClient) {
        return new DefaultValidatedRequestAction<>(consentRequestServiceClient);
    }


    @Bean("consentRequestOrchestrator")
    public RequestOrchestrator<ConsentRequestServiceClient> consentRequestOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            ConsentRequestServiceClient consentRequestServiceClient,
            DefaultValidatedRequestAction<ConsentRequestServiceClient> consentRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                consentRequestServiceClient,
                consentRequestAction);
    }

    @Bean("consentFetchRequestAction")
    public DefaultValidatedRequestAction<ConsentFetchServiceClient> consentFetchRequestAction(
            ConsentFetchServiceClient consentFetchServiceClient) {
        return new DefaultValidatedRequestAction<>(consentFetchServiceClient);
    }

    @Bean("consentFetchRequestOrchestrator")
    public RequestOrchestrator<ConsentFetchServiceClient> consentFetchOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            ConsentFetchServiceClient consentFetchServiceClient,
            DefaultValidatedRequestAction<ConsentFetchServiceClient> consentRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                consentFetchServiceClient,
                consentRequestAction);
    }

    @Bean("consentFetchResponseAction")
    public DefaultValidatedResponseAction<ConsentFetchServiceClient> consentFetchResponseAction(
            ConsentFetchServiceClient consentFetchServiceClient) {
        return new DefaultValidatedResponseAction<>(consentFetchServiceClient);
    }

    @Bean("consentFetchResponseOrchestrator")
    public ResponseOrchestrator consentFetchResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<ConsentFetchServiceClient> consentFetchResponseAction) {
        return new ResponseOrchestrator(validator, consentFetchResponseAction);
    }

    @Bean("consentStatusRequestAction")
    public DefaultValidatedRequestAction<ConsentStatusServiceClient> consentStatusRequestAction(
            ConsentStatusServiceClient consentStatusServiceClient) {
        return new DefaultValidatedRequestAction<>(consentStatusServiceClient);
    }

    @Bean("consentStatusRequestOrchestrator")
    public RequestOrchestrator<ConsentStatusServiceClient> consentStatusOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            ConsentStatusServiceClient consentStatusServiceClient,
            DefaultValidatedRequestAction<ConsentStatusServiceClient> consentStatusRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                consentStatusServiceClient,
                consentStatusRequestAction);
    }

    @Bean("consentStatusResponseAction")
    public DefaultValidatedResponseAction<ConsentStatusServiceClient> consentStatusResponseAction(
            ConsentStatusServiceClient consentStatusServiceClient) {
        return new DefaultValidatedResponseAction<>(consentStatusServiceClient);
    }

    @Bean("consentStatusResponseOrchestrator")
    public ResponseOrchestrator consentStatusResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<ConsentStatusServiceClient> consentStatusResponseAction) {
        return new ResponseOrchestrator(validator, consentStatusResponseAction);
    }

    @Bean("patientSearchRequestAction")
    public DefaultValidatedRequestAction<PatientSearchServiceClient> patientSearchRequestAction(
            PatientSearchServiceClient patientSearchServiceClient) {
        return new DefaultValidatedRequestAction<>(patientSearchServiceClient);
    }

    @Bean("patientSearchOrchestrator")
    public RequestOrchestrator<PatientSearchServiceClient> patientSearchOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            PatientSearchServiceClient patientSearchServiceClient,
            DefaultValidatedRequestAction<PatientSearchServiceClient> patientSearchRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                patientSearchServiceClient,
                patientSearchRequestAction);
    }

    @Bean
    public IdentityServiceClient clientRegistryClient(
            @Qualifier("customBuilder") WebClient.Builder builder,
            IdentityProperties identityProperties) {
        return new IdentityServiceClient(builder, identityProperties.getUrl(), identityProperties.getRealm());
    }

    @Bean
    public IdentityService centralRegistry(IdentityProperties identityProperties,
                                           IdentityServiceClient identityServiceClient,
                                           @Qualifier("accessToken") CacheAdapter<String, String> accessToken) {
        return new IdentityService(identityServiceClient, identityProperties, accessToken);
    }

    @Bean
    public HipConsentNotifyServiceClient hipConsentNotifyServiceClient(
            ServiceOptions serviceOptions,
            @Qualifier("customBuilder") WebClient.Builder builder,
            IdentityService identityService,
            CMRegistry cmRegistry,
            BridgeRegistry bridgeRegistry) {
        return new HipConsentNotifyServiceClient(serviceOptions, builder, identityService, cmRegistry, bridgeRegistry);
    }

    @Bean("hipConsentNotifyRequestAction")
    public DefaultValidatedRequestAction<HipConsentNotifyServiceClient> hipConsentNotifyRequestAction(
            HipConsentNotifyServiceClient hipConsentNotifyServiceClient) {
        return new DefaultValidatedRequestAction<>(hipConsentNotifyServiceClient);
    }

    @Bean("hipConsentNotifyRequestOrchestrator")
    public RequestOrchestrator<HipConsentNotifyServiceClient> hipConsentNotifyRequestOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            HipConsentNotifyServiceClient hipConsentNotifyServiceClient,
            DefaultValidatedRequestAction<HipConsentNotifyServiceClient> hipConsentNotifyRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                hipConsentNotifyServiceClient,
                hipConsentNotifyRequestAction);
    }

    @Bean
    public HiuConsentNotifyServiceClient hiuConsentNotifyServiceClient(
            ServiceOptions serviceOptions,
            @Qualifier("customBuilder") WebClient.Builder builder,
            IdentityService identityService,
            CMRegistry cmRegistry,
            BridgeRegistry bridgeRegistry) {
        return new HiuConsentNotifyServiceClient(serviceOptions, builder, identityService, cmRegistry, bridgeRegistry);
    }

    @Bean("hiuConsentNotifyRequestAction")
    public DefaultValidatedRequestAction<HiuConsentNotifyServiceClient> hiuConsentNotifyRequestAction(
            HiuConsentNotifyServiceClient hiuConsentNotifyServiceClient) {
        return new DefaultValidatedRequestAction<>(hiuConsentNotifyServiceClient);
    }

    @Bean("hiuConsentNotifyRequestOrchestrator")
    public RequestOrchestrator<HiuConsentNotifyServiceClient> hiuConsentNotifyRequestOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            HiuConsentNotifyServiceClient hiuConsentNotifyServiceClient,
            DefaultValidatedRequestAction<HiuConsentNotifyServiceClient> hiuConsentNotifyRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                hiuConsentNotifyServiceClient,
                hiuConsentNotifyRequestAction);
    }

    @Bean("consentResponseAction")
    public DefaultValidatedResponseAction<ConsentRequestServiceClient> consentResponseAction(
            ConsentRequestServiceClient consentRequestServiceClient) {
        return new DefaultValidatedResponseAction<>(consentRequestServiceClient);
    }

    @Bean("consentResponseOrchestrator")
    public ResponseOrchestrator consentResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<ConsentRequestServiceClient> consentResponseAction) {
        return new ResponseOrchestrator(validator, consentResponseAction);
    }

    @Bean
    public SubscriptionRequestServiceClient subscriptionRequestServiceClient(
            ServiceOptions serviceOptions,
            @Qualifier("customBuilder") WebClient.Builder builder,
            BridgeRegistry bridgeRegistry,
            IdentityService identityService,
            CMRegistry cmRegistry) {
        return new SubscriptionRequestServiceClient(serviceOptions, builder, identityService, bridgeRegistry, cmRegistry);
    }

    @Bean("subscriptionRequestAction")
    public DefaultValidatedRequestAction<SubscriptionRequestServiceClient> subscriptionRequestAction(
            SubscriptionRequestServiceClient subscriptionRequestServiceClient) {
        return new DefaultValidatedRequestAction<>(subscriptionRequestServiceClient);
    }

    @Bean("subscriptionRequestOrchestrator")
    public RequestOrchestrator<SubscriptionRequestServiceClient> subscriptionRequestOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            SubscriptionRequestServiceClient subscriptionRequestServiceClient,
            DefaultValidatedRequestAction<SubscriptionRequestServiceClient> subscriptionRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                subscriptionRequestServiceClient,
                subscriptionRequestAction);
    }

    @Bean("subscriptionResponseAction")
    public DefaultValidatedResponseAction<SubscriptionRequestServiceClient> subscriptionResponseAction(
            SubscriptionRequestServiceClient subscriptionRequestServiceClient) {
        return new DefaultValidatedResponseAction<>(subscriptionRequestServiceClient);
    }

    @Bean("subscriptionResponseOrchestrator")
    public ResponseOrchestrator subscriptionResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<SubscriptionRequestServiceClient> subscriptionResponseAction) {
        return new ResponseOrchestrator(validator, subscriptionResponseAction);
    }

    @Bean
    public SubscriptionRequestNotifyServiceClient subscriptionRequestNotifyServiceClient(
            ServiceOptions serviceOptions,
            @Qualifier("customBuilder") WebClient.Builder builder,
            BridgeRegistry bridgeRegistry,
            IdentityService identityService,
            CMRegistry cmRegistry) {
        return new SubscriptionRequestNotifyServiceClient(serviceOptions, builder, identityService, bridgeRegistry, cmRegistry);
    }

    @Bean("subscriptionRequestNotifyAction")
    public DefaultValidatedRequestAction<SubscriptionRequestNotifyServiceClient> subscriptionRequestNotifyAction(
            SubscriptionRequestNotifyServiceClient subscriptionRequestNotifyServiceClient) {
        return new DefaultValidatedRequestAction<>(subscriptionRequestNotifyServiceClient);
    }

    @Bean("subscriptionRequestNotifyOrchestrator")
    public RequestOrchestrator<SubscriptionRequestNotifyServiceClient> subscriptionRequestNotifyOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            SubscriptionRequestNotifyServiceClient subscriptionRequestNotifyServiceClient,
            DefaultValidatedRequestAction<SubscriptionRequestNotifyServiceClient> subscriptionRequestNotifyAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                subscriptionRequestNotifyServiceClient,
                subscriptionRequestNotifyAction);
    }

    @Bean("subscriptionRequestNotifyResponseAction")
    public DefaultValidatedResponseAction<SubscriptionRequestNotifyServiceClient> subscriptionRequestNotifyResponseAction(
            SubscriptionRequestNotifyServiceClient subscriptionRequestNotifyServiceClient) {
        return new DefaultValidatedResponseAction<>(subscriptionRequestNotifyServiceClient);
    }

    @Bean("subscriptionRequestNotifyResponseOrchestrator")
    public ResponseOrchestrator subscriptionRequestNotifyResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<SubscriptionRequestNotifyServiceClient> subscriptionRequestNotifyResponseAction) {
        return new ResponseOrchestrator(validator, subscriptionRequestNotifyResponseAction);
    }

    @Bean
    public PatientSearchServiceClient patientSearchServiceClient(
            ServiceOptions serviceOptions,
            @Qualifier("customBuilder") WebClient.Builder builder,
            IdentityService identityService,
            BridgeRegistry bridgeRegistry,
            CMRegistry cmRegistry) {
        return new PatientSearchServiceClient(serviceOptions, builder, identityService, bridgeRegistry, cmRegistry);
    }

    @Bean("patientSearchResponseAction")
    public DefaultValidatedResponseAction<PatientSearchServiceClient> patientSearchResponseAction(
            PatientSearchServiceClient patientSearchServiceClient) {
        return new DefaultValidatedResponseAction<>(patientSearchServiceClient);
    }

    @Bean("patientSearchResponseOrchestrator")
    public ResponseOrchestrator patientSearchResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<PatientSearchServiceClient> patientSearchResponseAction) {
        return new ResponseOrchestrator(validator, patientSearchResponseAction);
    }

    @Bean
    public DataFlowRequestServiceClient dataFlowRequestServiceClient(
            ServiceOptions serviceOptions,
            @Qualifier("customBuilder") WebClient.Builder builder,
            IdentityService identityService,
            BridgeRegistry bridgeRegistry,
            CMRegistry cmRegistry) {
        return new DataFlowRequestServiceClient(serviceOptions, builder, identityService, bridgeRegistry, cmRegistry);
    }

    @Bean("dataflowRequestAction")
    public DefaultValidatedRequestAction<DataFlowRequestServiceClient> dataflowRequestAction(
            DataFlowRequestServiceClient dataFlowRequestServiceClient) {
        return new DefaultValidatedRequestAction<>(dataFlowRequestServiceClient);
    }

    @Bean("dataFlowRequestOrchestrator")
    public RequestOrchestrator<DataFlowRequestServiceClient> dataFlowRequestOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            DataFlowRequestServiceClient dataFlowRequestServiceClient,
            DefaultValidatedRequestAction<DataFlowRequestServiceClient> dataflowRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                dataFlowRequestServiceClient,
                dataflowRequestAction);
    }

    @Bean("dataFlowRequestResponseAction")
    public DefaultValidatedResponseAction<DataFlowRequestServiceClient> dataFlowRequestResponseAction(
            DataFlowRequestServiceClient dataFlowRequestServiceClient) {
        return new DefaultValidatedResponseAction<>(dataFlowRequestServiceClient);
    }

    @Bean("dataFlowRequestResponseOrchestrator")
    public ResponseOrchestrator dataFlowRequestResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<DataFlowRequestServiceClient> dataFlowRequestResponseAction) {
        return new ResponseOrchestrator(validator, dataFlowRequestResponseAction);
    }

    @Bean
    public HealthInfoNotificationServiceClient healthInformationRequestServiceClient(
            ServiceOptions serviceOptions,
            @Qualifier("customBuilder") WebClient.Builder builder,
            IdentityService centralRegistry,
            CMRegistry cmRegistry) {
        return new HealthInfoNotificationServiceClient(serviceOptions, builder, centralRegistry, cmRegistry);
    }

    @Bean("healthInfoNotificationRequestAction")
    public DefaultValidatedRequestAction<HealthInfoNotificationServiceClient> healthInfoNotificationRequestAction(
            HealthInfoNotificationServiceClient healthInfoNotificationServiceClient) {
        return new DefaultValidatedRequestAction<>(healthInfoNotificationServiceClient);
    }

    @Bean("healthInfoNotificationOrchestrator")
    public RequestOrchestrator<HealthInfoNotificationServiceClient> healthInfoNotificationOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            HealthInfoNotificationServiceClient healthInfoNotificationServiceClient,
            DefaultValidatedRequestAction<HealthInfoNotificationServiceClient> healthInfoNotificationRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                healthInfoNotificationServiceClient,
                healthInfoNotificationRequestAction);
    }

    @Bean
    public HipDataFlowServiceClient hipDataFlowServiceClient(ServiceOptions serviceOptions,
                                                             @Qualifier("customBuilder") WebClient.Builder builder,
                                                             CMRegistry cmRegistry,
                                                             IdentityService identityService,
                                                             BridgeRegistry bridgeRegistry) {
        return new HipDataFlowServiceClient(serviceOptions, builder, identityService, cmRegistry, bridgeRegistry);
    }

    @Bean("defaultHipDataflowRequestAction")
    public DefaultValidatedRequestAction<HipDataFlowServiceClient> defaultHipDataFlowRequestAction(
            HipDataFlowServiceClient hipDataFlowServiceClient) {
        return new DefaultValidatedRequestAction<>(hipDataFlowServiceClient);
    }

    @Bean("hipDataflowRequestAction")
    public RetryableValidatedRequestAction<HipDataFlowServiceClient> hipDataflowRequestAction(
            DefaultValidatedRequestAction<HipDataFlowServiceClient> defaultHipDataflowRequestAction,
            ReceiverOptions receiverOptions,
            SenderOptions senderOptions,
            ServiceOptions serviceOptions) {
        return new RetryableValidatedRequestAction<>(RabbitFlux.createReceiver(receiverOptions),
                RabbitFlux.createSender(senderOptions),
                defaultHipDataflowRequestAction,
                serviceOptions,
                GW_DATAFLOW_QUEUE,
                X_HIP_ID);
    }

    @Bean("hipDataflowRequestOrchestrator")
    public RequestOrchestrator<HipDataFlowServiceClient> hipDataflowRequestOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            HipDataFlowServiceClient hipDataFlowServiceClient,
            RetryableValidatedRequestAction<HipDataFlowServiceClient> hipDataflowRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                hipDataFlowServiceClient,
                hipDataflowRequestAction);
    }

    @Bean
    public AuthConfirmServiceClient authConfirmServiceClient(
            ServiceOptions serviceOptions,
            @Qualifier("customBuilder") WebClient.Builder builder,
            BridgeRegistry bridgeRegistry,
            IdentityService identityService,
            CMRegistry cmRegistry) {
        return new AuthConfirmServiceClient(serviceOptions, builder, identityService, bridgeRegistry, cmRegistry);
    }

    @Bean("authConfirmDefaultValidatedRequestAction")
    public DefaultValidatedRequestAction<AuthConfirmServiceClient> authConfirmDefaultValidatedRequestAction(
            AuthConfirmServiceClient authConfirmServiceClient) {
        return new DefaultValidatedRequestAction<>(authConfirmServiceClient);
    }

    @Bean("authConfirmRequestOrchestrator")
    public RequestOrchestrator<AuthConfirmServiceClient> authConfirmRequestOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            AuthConfirmServiceClient authConfirmServiceClient,
            DefaultValidatedRequestAction<AuthConfirmServiceClient> authConfirmRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                authConfirmServiceClient,
                authConfirmRequestAction);
    }

    @Bean("authConfirmResponseAction")
    public DefaultValidatedResponseAction<AuthConfirmServiceClient> authConfirmResponseAction(
            AuthConfirmServiceClient authConfirmServiceClient) {
        return new DefaultValidatedResponseAction<>(authConfirmServiceClient);
    }

    @Bean("authConfirmResponseOrchestrator")
    public ResponseOrchestrator authConfirmResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<AuthConfirmServiceClient> authConfirmResponseAction) {
        return new ResponseOrchestrator(validator, authConfirmResponseAction);
    }

    @Bean
    public ConnectionFactory connectionFactory(RabbitmqOptions rabbitmqOptions) throws KeyManagementException, NoSuchAlgorithmException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(rabbitmqOptions.getHost());
        connectionFactory.setPort(rabbitmqOptions.getPort());
        connectionFactory.setUsername(rabbitmqOptions.getUsername());
        connectionFactory.setPassword(rabbitmqOptions.getPassword());
        if(rabbitmqOptions.isUseSSL()){
            connectionFactory.useSslProtocol();
        }
        connectionFactory.useNio();
        return connectionFactory;
    }

    @Bean
    public ReceiverOptions receiverOptions(ConnectionFactory connectionFactory){
        return new ReceiverOptions()
                .connectionFactory(connectionFactory)
                .connectionSubscriptionScheduler(Schedulers.elastic());
    }

    @Bean
    public SenderOptions senderOptions(ConnectionFactory connectionFactory, RabbitmqOptions rabbitmqOptions) {
        Mono<? extends Connection> connection = singleConnectionMono(connectionFactory);
        return new SenderOptions()
                .connectionFactory(connectionFactory)
                .channelPool(ChannelPoolFactory.createChannelPool(
                        connection,
                        new ChannelPoolOptions().maxCacheSize(rabbitmqOptions.getChannelPoolMaxCacheSize()))
                )
                .resourceManagementScheduler(Schedulers.elastic());
    }

    @Bean("hipDataFlowRequestResponseAction")
    public DefaultValidatedResponseAction<HipDataFlowServiceClient> hipDataFlowRequestResponseAction(
            HipDataFlowServiceClient hipDataFlowServiceClient) {
        return new DefaultValidatedResponseAction<>(hipDataFlowServiceClient);
    }

    @Bean("hipDataFlowRequestResponseOrchestrator")
    public ResponseOrchestrator hipDataFlowRequestResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<HipDataFlowServiceClient> hipDataFlowRequestResponseAction) {
        return new ResponseOrchestrator(validator, hipDataFlowRequestResponseAction);
    }

    @Bean("hipConsentNotifyResponseAction")
    public DefaultValidatedResponseAction<HipConsentNotifyServiceClient> hipConsentNotifyResponseAction(
            HipConsentNotifyServiceClient hipConsentNotifyServiceClient) {
        return new DefaultValidatedResponseAction<>(hipConsentNotifyServiceClient);
    }

    @Bean("hiuConsentNotifyResponseAction")
    public DefaultValidatedResponseAction<HiuConsentNotifyServiceClient> hiuConsentNotifyResponseAction(
            HiuConsentNotifyServiceClient hiuConsentNotifyServiceClient) {
        return new DefaultValidatedResponseAction<>(hiuConsentNotifyServiceClient);
    }

    @Bean("hipConsentNotifyResponseOrchestrator")
    public ResponseOrchestrator hipConsentNotifyResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<HipConsentNotifyServiceClient> hipConsentNotifyResponseAction) {
        return new ResponseOrchestrator(validator, hipConsentNotifyResponseAction);
    }

    @Bean("hiuConsentNotifyResponseOrchestrator")
    public ResponseOrchestrator hiuConsentNotifyResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<HiuConsentNotifyServiceClient> hiuConsentNotifyResponseAction) {
        return new ResponseOrchestrator(validator, hiuConsentNotifyResponseAction);
    }

    @Bean
    public Heartbeat heartbeat(RabbitmqOptions rabbitmqOptions,
                               IdentityProperties identityProperties,
                               CacheHealth cacheHealth) {
        return new Heartbeat(rabbitmqOptions, identityProperties, cacheHealth);
    }

    @Bean("gatewayHttpConnector")
    @ConditionalOnProperty(value = "webclient.use-connection-pool", havingValue = "false")
    public ClientHttpConnector clientHttpConnector() {
        return new ReactorClientHttpConnector(HttpClient.create(ConnectionProvider.newConnection()));
    }

    @Bean("gatewayHttpConnector")
    @ConditionalOnProperty(value = "webclient.use-connection-pool", havingValue = "true")
    public ClientHttpConnector pooledClientHttpConnector(WebClientOptions webClientOptions) {
        return new ReactorClientHttpConnector(
                HttpClient.create(
                        ConnectionProvider.builder("gateway-http-connection-pool")
                                .maxConnections(webClientOptions.getPoolSize())
                                .maxLifeTime(Duration.ofMinutes(webClientOptions.getMaxLifeTime()))
                                .maxIdleTime(Duration.ofMinutes(webClientOptions.getMaxIdleTimeout()))
                                .build()
                )
        );
    }

    @Bean("customBuilder")
    public WebClient.Builder webClient(
            @Qualifier("gatewayHttpConnector") final ClientHttpConnector clientHttpConnector,
            ObjectMapper objectMapper) {
        return WebClient
                .builder()
                .exchangeStrategies(exchangeStrategies(objectMapper))
                .clientConnector(clientHttpConnector)
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(maxInMemorySize));
    }

    private ExchangeStrategies exchangeStrategies(ObjectMapper objectMapper) {
        var encoder = new Jackson2JsonEncoder(objectMapper);
        var decoder = new Jackson2JsonDecoder(objectMapper);
        return ExchangeStrategies
                .builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonEncoder(encoder);
                    configurer.defaultCodecs().jackson2JsonDecoder(decoder);
                }).build();
    }

    @Bean("readWriteClient")
    public PgPool readWriteClient(DbOptions dbOptions) {
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(dbOptions.getPort())
                .setHost(dbOptions.getHost())
                .setDatabase(dbOptions.getSchema())
                .setUser(dbOptions.getUser())
                .setPassword(dbOptions.getPassword());

        PoolOptions poolOptions = new PoolOptions().setMaxSize(dbOptions.getPoolSize());
        return PgPool.pool(connectOptions, poolOptions);
    }

    @Bean("readOnlyClient")
    public PgPool readOnlyClient(DbOptions dbOptions) {
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(dbOptions.getReplica().getPort())
                .setHost(dbOptions.getReplica().getHost())
                .setDatabase(dbOptions.getSchema())
                .setUser(dbOptions.getReplica().getUser())
                .setPassword(dbOptions.getReplica().getPassword());

        PoolOptions poolOptions = new PoolOptions().setMaxSize(dbOptions.getReplica().getPoolSize());
        return PgPool.pool(connectOptions, poolOptions);
    }

    @Bean
    public MappingRepository mappingRepository(@Qualifier("readOnlyClient") PgPool readOnlyClient) {
        return new MappingRepository(readOnlyClient);
    }

    @Bean
    public RedundantRequestValidator redundantRequestValidator(
            @Qualifier("requestIdTimestampMappings") CacheAdapter<String, String> requestIdTimestampMappings,
            RedisOptions redisOptions,
            @Value("${gateway.cacheMethod}") String cacheMethod) {
        return new RedundantRequestValidator(requestIdTimestampMappings,
                "redis".equalsIgnoreCase(cacheMethod)
                ? String.format("%s_replay", redisOptions.getRootNamespace())
                : null);
    }

    @Bean
    public AdminServiceClient adminClientRegistryClient(@Qualifier("customBuilder") WebClient.Builder builder,
                                                        IdentityProperties identityProperties,
                                                        IdentityService identityService) {
        return new AdminServiceClient(builder,
                identityProperties.getUrl(),
                identityProperties.getRealm(),
                identityService::tokenForAdmin);
    }

    @Bean
    public RegistryRepository registryRepository(@Qualifier("readWriteClient") PgPool readWriteClient,
                                                 @Qualifier("readOnlyClient") PgPool readOnlyClient) {
        return new RegistryRepository(readWriteClient, readOnlyClient);
    }

    @Bean
    public RegistryService registryService(RegistryRepository registryRepository,
                                           CacheAdapter<String, String> consentManagerMappings,
                                           CacheAdapter<String, String> bridgeMappings,
                                           AdminServiceClient adminServiceClient,
                                           FacilityRegistryClient facilityRegistryClient) {
        return new RegistryService(registryRepository, consentManagerMappings, bridgeMappings, adminServiceClient, facilityRegistryClient);
    }

    @Bean("userAuthenticatorClient")
    public UserAuthenticatorClient userAuthenticatorClient(ServiceOptions serviceOptions,
                                                           @Qualifier("customBuilder") WebClient.Builder builder,
                                                           CMRegistry cmRegistry,
                                                           IdentityService identityService,
                                                           BridgeRegistry bridgeRegistry) {
        return new UserAuthenticatorClient(serviceOptions, builder, identityService, cmRegistry, bridgeRegistry);
    }

    @Bean("userAuthenticationRequestAction")
    public DefaultValidatedRequestAction<UserAuthenticatorClient> userAuthenticationRequestAction(
            UserAuthenticatorClient userAuthenticatorClient) {
        return new DefaultValidatedRequestAction<>(userAuthenticatorClient);
    }

    @Bean("userAuthenticationRequestOrchestrator")
    public RequestOrchestrator<UserAuthenticatorClient> userAuthenticationRequestOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            UserAuthenticatorClient userAuthenticatorClient,
            DefaultValidatedRequestAction<UserAuthenticatorClient> userAuthenticationRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                userAuthenticatorClient,
                userAuthenticationRequestAction);
    }

    @Bean("userAuthenticationResponseAction")
    public DefaultValidatedResponseAction<UserAuthenticatorClient> userAuthenticationResponseAction(
            UserAuthenticatorClient userAuthenticatorClient) {
        return new DefaultValidatedResponseAction<>(userAuthenticatorClient);
    }

    @Bean("userAuthenticationResponseOrchestrator")
    public ResponseOrchestrator userAuthenticationResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<UserAuthenticatorClient> userAuthenticationResponseAction) {
        return new ResponseOrchestrator(validator, userAuthenticationResponseAction);
    }

    @Bean("hipInitLinkServiceClient")
    public HipInitLinkServiceClient hipInitLinkServiceClient(ServiceOptions serviceOptions,
                                                             @Qualifier("customBuilder") WebClient.Builder builder,
                                                             CMRegistry cmRegistry,
                                                             IdentityService identityService,
                                                             BridgeRegistry bridgeRegistry) {
        return new HipInitLinkServiceClient(serviceOptions, builder, identityService, cmRegistry, bridgeRegistry);
    }

    @Bean("hipInitLinkRequestAction")
    public DefaultValidatedRequestAction<HipInitLinkServiceClient> hipInitLinkRequestAction(
            HipInitLinkServiceClient hipInitLinkServiceClient) {
        return new DefaultValidatedRequestAction<>(hipInitLinkServiceClient);
    }

    @Bean("hipInitLinkRequestOrchestrator")
    public RequestOrchestrator<HipInitLinkServiceClient> hipInitLinkRequestOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            HipInitLinkServiceClient hipInitLinkServiceClient,
            DefaultValidatedRequestAction<HipInitLinkServiceClient> hipInitLinkRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                hipInitLinkServiceClient,
                hipInitLinkRequestAction);
    }

    @Bean("hipInitLinkResponseAction")
    public DefaultValidatedResponseAction<HipInitLinkServiceClient> hipInitLinkResponseAction(
            HipInitLinkServiceClient hipInitLinkServiceClient) {
        return new DefaultValidatedResponseAction<>(hipInitLinkServiceClient);
    }

    @Bean("hipInitLinkResponseOrchestrator")
    public ResponseOrchestrator hipInitLinkResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<HipInitLinkServiceClient> hipInitLinkResponseAction) {
        return new ResponseOrchestrator(validator, hipInitLinkResponseAction);
    }

    @Bean("patientServiceClient")
    public PatientServiceClient patientServiceClient(ServiceOptions serviceOptions,
                                                     @Qualifier("customBuilder") WebClient.Builder builder,
                                                     CMRegistry cmRegistry,
                                                     IdentityService identityService,
                                                     BridgeRegistry bridgeRegistry) {
        return new PatientServiceClient(serviceOptions, builder, identityService, cmRegistry, bridgeRegistry);
    }

    @Bean("patientRequestAction")
    public DefaultValidatedRequestAction<PatientServiceClient> patientRequestAction(
            PatientServiceClient patientServiceClient) {
        return new DefaultValidatedRequestAction<>(patientServiceClient);
    }

    @Bean("patientRequestOrchestrator")
    public RequestOrchestrator<PatientServiceClient> patientRequestOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            PatientServiceClient patientServiceClient,
            DefaultValidatedRequestAction<PatientServiceClient> patientRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                patientServiceClient,
                patientRequestAction);
    }

    @Bean("patientResponseAction")
    public DefaultValidatedResponseAction<PatientServiceClient> patientResponseAction(
            PatientServiceClient patientServiceClient) {
        return new DefaultValidatedResponseAction<>(patientServiceClient);
    }

    @Bean("patientResponseOrchestrator")
    public ResponseOrchestrator patientResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<PatientServiceClient> patientResponseAction) {
        return new ResponseOrchestrator(validator, patientResponseAction);
    }

    @Bean("authModeFetchClient")
    public AuthModeFetchClient authModeFetchClient(ServiceOptions serviceOptions,
                                                     @Qualifier("customBuilder") WebClient.Builder builder,
                                                     CMRegistry cmRegistry,
                                                     IdentityService identityService,
                                                     BridgeRegistry bridgeRegistry) {
        return new AuthModeFetchClient(serviceOptions, builder, identityService, bridgeRegistry, cmRegistry);
    }

    @Bean("authModeFetchRequestAction")
    public DefaultValidatedRequestAction<AuthModeFetchClient> authModeFetchRequestAction(
            AuthModeFetchClient authModeFetchClient) {
        return new DefaultValidatedRequestAction<>(authModeFetchClient);
    }

    @Bean("authModeFetchRequestOrchestrator")
    public RequestOrchestrator<AuthModeFetchClient> authModeFetchRequestOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            AuthModeFetchClient authModeFetchClient,
            DefaultValidatedRequestAction<AuthModeFetchClient> authModeFetchRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                authModeFetchClient,
                authModeFetchRequestAction);
    }

    @Bean("authModeFetchResponseAction")
    public DefaultValidatedResponseAction<AuthModeFetchClient> authModeFetchResponseAction(
            AuthModeFetchClient authModeFetchClient) {
        return new DefaultValidatedResponseAction<>(authModeFetchClient);
    }

    @Bean("authModeFetchResponseOrchestrator")
    public ResponseOrchestrator authModeFetchResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<AuthModeFetchClient> authModeFetchResponseAction) {
        return new ResponseOrchestrator(validator, authModeFetchResponseAction);
    }

    @Bean("authNotifyServiceClient")
    public AuthNotifyServiceClient authNotifyServiceClient(ServiceOptions serviceOptions,
                                                           @Qualifier("customBuilder") WebClient.Builder builder,
                                                           CMRegistry cmRegistry,
                                                           IdentityService identityService,
                                                           BridgeRegistry bridgeRegistry) {
        return new AuthNotifyServiceClient(serviceOptions, builder, identityService, cmRegistry, bridgeRegistry);
    }

    @Bean("authNotifyRequestAction")
    public DefaultValidatedRequestAction<AuthNotifyServiceClient> authNotifyRequestAction(
            AuthNotifyServiceClient authNotifyServiceClient) {
        return new DefaultValidatedRequestAction<>(authNotifyServiceClient);
    }

    @Bean("authNotifyRequestOrchestrator")
    public RequestOrchestrator<AuthNotifyServiceClient> authNotifyHelper(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            AuthNotifyServiceClient authNotifyServiceClient,
            DefaultValidatedRequestAction<AuthNotifyServiceClient> authNotifyRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                authNotifyServiceClient,
                authNotifyRequestAction);
    }

    @Bean("authNotifyResponseAction")
    public DefaultValidatedResponseAction<AuthNotifyServiceClient> authNotifyResponseAction(
            AuthNotifyServiceClient authNotifyServiceClient) {
        return new DefaultValidatedResponseAction<>(authNotifyServiceClient);
    }

    @Bean("authNotifyResponseOrchestrator")
    public ResponseOrchestrator authNotifyResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<AuthNotifyServiceClient> authNotifyResponseAction) {
        return new ResponseOrchestrator(validator, authNotifyResponseAction);
    }

    @Bean
    // This exception handler needs to be given highest priority compared to DefaultErrorWebExceptionHandler, hence order = -2.
    @Order(-2)
    public GlobalExceptionHandler clientErrorExceptionHandler(ErrorAttributes errorAttributes,
                                                              ResourceProperties resourceProperties,
                                                              ApplicationContext applicationContext,
                                                              ServerCodecConfigurer serverCodecConfigurer) {

        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler(errorAttributes,
                resourceProperties, applicationContext);
        globalExceptionHandler.setMessageWriters(serverCodecConfigurer.getWriters());
        return globalExceptionHandler;
    }

    @Bean
    @ConditionalOnProperty(value = "gateway.disableHttpOptionsMethod", havingValue = "true")
    public WebFilter disableOptionsMethodFilter(){
        return (exchange, chain) -> {
            if(exchange.getRequest().getMethod().equals(HttpMethod.OPTIONS)) {
                exchange.getResponse().setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
                return Mono.empty();
            }
            return chain.filter(exchange);
        };
    }


    @Bean
    public HiuSubscriptionNotifyServiceClient hiuSubscriptionNotifyServiceClient(
            ServiceOptions serviceOptions,
            @Qualifier("customBuilder") WebClient.Builder builder,
            IdentityService identityService,
            CMRegistry cmRegistry,
            BridgeRegistry bridgeRegistry) {
        return new HiuSubscriptionNotifyServiceClient(serviceOptions, builder, identityService, cmRegistry, bridgeRegistry);
    }

    @Bean("hiuSubscriptionNotifyResponseAction")
    public DefaultValidatedResponseAction<HiuSubscriptionNotifyServiceClient> hiuSubscriptionNotifyResponseAction(
            HiuSubscriptionNotifyServiceClient hiuSubscriptionNotifyServiceClient) {
        return new DefaultValidatedResponseAction<>(hiuSubscriptionNotifyServiceClient);
    }

    @Bean("hiuSubscriptionNotifyResponseOrchestrator")
    public ResponseOrchestrator hiuSubscriptionNotifyResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<HiuSubscriptionNotifyServiceClient> hiuSubscriptionNotifyResponseAction) {
        return new ResponseOrchestrator(validator, hiuSubscriptionNotifyResponseAction);
    }

    @Bean("hiuSubscriptionNotifyRequestAction")
    public DefaultValidatedRequestAction<HiuSubscriptionNotifyServiceClient> hiuSubscriptionNotifyRequestAction(
            HiuSubscriptionNotifyServiceClient hiuSubscriptionNotifyServiceClient) {
        return new DefaultValidatedRequestAction<>(hiuSubscriptionNotifyServiceClient);
    }

    @Bean("hiuSubscriptionNotifyRequestOrchestrator")
    public RequestOrchestrator<HiuSubscriptionNotifyServiceClient> hiuSubscriptionNotifyRequestOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            HiuSubscriptionNotifyServiceClient hiuConsentNotifyServiceClient,
            DefaultValidatedRequestAction<HiuSubscriptionNotifyServiceClient> hiuSubscriptionNotifyRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                hiuConsentNotifyServiceClient,
                hiuSubscriptionNotifyRequestAction);
    }

    @Bean("facilityRegistryClient")
    public FacilityRegistryClient facilityRegistryClient(@Qualifier("customBuilder") WebClient.Builder builder,
                                                         FacilityRegistryProperties facilityRegistryProperties,
                                                         @Qualifier("facilityTokenCache") CacheAdapter<String, String> facilityTokenCache){
        return new FacilityRegistryClient(builder, facilityRegistryProperties, facilityTokenCache);
    }

    @Bean("patientSMSNotificationClient")
    public PatientSMSNotificationClient patientSMSNotificationClient(ServiceOptions serviceOptions,
                                                                     @Qualifier("customBuilder") WebClient.Builder builder,
                                                                     CMRegistry cmRegistry,
                                                                     IdentityService identityService,
                                                                     BridgeRegistry bridgeRegistry) {
        return new PatientSMSNotificationClient(serviceOptions, builder, identityService, cmRegistry, bridgeRegistry);
    }

    @Bean("patientSMSNotifcationRequestAction")
    public DefaultValidatedRequestAction<PatientSMSNotificationClient> patientSMSNotificationRequestAction(
            PatientSMSNotificationClient patientSMSNotificationClient) {
        return new DefaultValidatedRequestAction<>(patientSMSNotificationClient);
    }

    @Bean("patientSMSNotifyRequestOrchestrator")
    public RequestOrchestrator<PatientSMSNotificationClient> patientSMSNotifyRequestOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            PatientSMSNotificationClient patientSMSNotificationClient,
            DefaultValidatedRequestAction<PatientSMSNotificationClient> patientSMSNotificationRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                patientSMSNotificationClient,
                patientSMSNotificationRequestAction);
    }

    @Bean("patientSMSNotificationResponseAction")
    public DefaultValidatedResponseAction<PatientSMSNotificationClient> patientSMSNotificationResponseAction(
            PatientSMSNotificationClient patientSMSNotificationClient) {
        return new DefaultValidatedResponseAction<>(patientSMSNotificationClient);
    }

    @Bean("patientSMSNotifyResponseOrchestrator")
    public ResponseOrchestrator patientSMSNotifyResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<PatientSMSNotificationClient> patientSMSNotificationResponseAction) {
        return new ResponseOrchestrator(validator, patientSMSNotificationResponseAction);
    }

    @Bean("hipDataNotificationServiceClient")
    public HipDataNotificationServiceClient hipDataNotificationServiceClient(ServiceOptions serviceOptions,
                                                                             @Qualifier("customBuilder") WebClient.Builder builder,
                                                                             CMRegistry cmRegistry,
                                                                             IdentityService identityService,
                                                                             BridgeRegistry bridgeRegistry) {
        return new HipDataNotificationServiceClient(serviceOptions, builder, identityService, cmRegistry, bridgeRegistry);
    }

    @Bean("hipDataNotificationRequestAction")
    public DefaultValidatedRequestAction<HipDataNotificationServiceClient> hipDataNotificationRequestAction(
            HipDataNotificationServiceClient hipDataNotificationServiceClient) {
        return new DefaultValidatedRequestAction<>(hipDataNotificationServiceClient);
    }

    @Bean("hipDataNotificationRequestOrchestrator")
    public RequestOrchestrator<HipDataNotificationServiceClient> hipDataNotificationRequestOrchestrator(
            @Qualifier("requestIdMappings") CacheAdapter<String, String> requestIdMappings,
            RedundantRequestValidator redundantRequestValidator,
            Validator validator,
            HipDataNotificationServiceClient hipDataNotificationServiceClient,
            DefaultValidatedRequestAction<HipDataNotificationServiceClient> hipDataNotificationRequestAction) {
        return new RequestOrchestrator<>(requestIdMappings,
                redundantRequestValidator,
                validator,
                hipDataNotificationServiceClient,
                hipDataNotificationRequestAction);
    }

    @Bean("hipDataNotificationResponseAction")
    public DefaultValidatedResponseAction<HipDataNotificationServiceClient> hipDataNotificationResponseAction(
            HipDataNotificationServiceClient hipDataNotificationServiceClient) {
        return new DefaultValidatedResponseAction<>(hipDataNotificationServiceClient);
    }

    @Bean("hipDataNotificationResponseOrchestrator")
    public ResponseOrchestrator hipDataNotificationResponseOrchestrator(
            Validator validator,
            DefaultValidatedResponseAction<HipDataNotificationServiceClient> hipDataNotificationResponseAction) {
        return new ResponseOrchestrator(validator, hipDataNotificationResponseAction);
    }
}
