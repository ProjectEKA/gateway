package in.projecteka.gateway;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.clients.IdentityProperties;
import in.projecteka.gateway.common.AdminAuthenticator;
import in.projecteka.gateway.common.Authenticator;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static in.projecteka.gateway.common.Constants.INTERNAL_BRIDGES;
import static in.projecteka.gateway.common.Constants.INTERNAL_BRIDGES_BRIDGE_ID_SERVICES;
import static in.projecteka.gateway.common.Constants.INTERNAL_CM;
import static in.projecteka.gateway.common.Constants.PATH_CARE_CONTEXTS_DISCOVER;
import static in.projecteka.gateway.common.Constants.PATH_CARE_CONTEXTS_ON_DISCOVER;
import static in.projecteka.gateway.common.Constants.PATH_CERTS;
import static in.projecteka.gateway.common.Constants.PATH_CONSENTS_FETCH;
import static in.projecteka.gateway.common.Constants.PATH_CONSENTS_HIP_NOTIFY;
import static in.projecteka.gateway.common.Constants.PATH_CONSENTS_HIP_ON_NOTIFY;
import static in.projecteka.gateway.common.Constants.PATH_CONSENTS_HIU_NOTIFY;
import static in.projecteka.gateway.common.Constants.PATH_CONSENTS_ON_FETCH;
import static in.projecteka.gateway.common.Constants.PATH_CONSENT_REQUESTS_INIT;
import static in.projecteka.gateway.common.Constants.PATH_CONSENT_REQUESTS_ON_INIT;
import static in.projecteka.gateway.common.Constants.PATH_HEALTH_INFORMATION_CM_ON_REQUEST;
import static in.projecteka.gateway.common.Constants.PATH_HEALTH_INFORMATION_CM_REQUEST;
import static in.projecteka.gateway.common.Constants.PATH_HEALTH_INFORMATION_HIP_ON_REQUEST;
import static in.projecteka.gateway.common.Constants.PATH_HEALTH_INFORMATION_HIP_REQUEST;
import static in.projecteka.gateway.common.Constants.PATH_HEALTH_INFORMATION_NOTIFY;
import static in.projecteka.gateway.common.Constants.PATH_HEARTBEAT;
import static in.projecteka.gateway.common.Constants.PATH_LINK_CONFIRM;
import static in.projecteka.gateway.common.Constants.PATH_LINK_INIT;
import static in.projecteka.gateway.common.Constants.PATH_LINK_ON_CONFIRM;
import static in.projecteka.gateway.common.Constants.PATH_LINK_ON_INIT;
import static in.projecteka.gateway.common.Constants.PATH_PATIENTS_FIND;
import static in.projecteka.gateway.common.Constants.PATH_PATIENTS_ON_FIND;
import static in.projecteka.gateway.common.Constants.PATH_SERVICE_URLS;
import static in.projecteka.gateway.common.Constants.PATH_SESSIONS;
import static in.projecteka.gateway.common.Constants.PATH_WELL_KNOWN_OPENID_CONFIGURATION;
import static in.projecteka.gateway.common.Constants.USER_SESSION;
import static in.projecteka.gateway.common.Constants.PATH_USERS_AUTH_INIT;
import static in.projecteka.gateway.common.Constants.PATH_USERS_AUTH_ON_INIT;
import static in.projecteka.gateway.common.Role.ADMIN;
import static in.projecteka.gateway.common.Role.CM;
import static in.projecteka.gateway.common.Role.HIP;
import static in.projecteka.gateway.common.Role.HIU;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.StringUtils.hasText;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    protected static final String[] HIU_APIS = new String[]{
            PATH_CONSENT_REQUESTS_INIT,
            PATH_CONSENTS_FETCH,
            PATH_PATIENTS_FIND,
            PATH_HEALTH_INFORMATION_CM_REQUEST
    };
    protected static final String[] HIP_APIS = new String[]{
            PATH_CARE_CONTEXTS_ON_DISCOVER,
            PATH_LINK_ON_INIT,
            PATH_LINK_ON_CONFIRM,
            PATH_CONSENTS_HIP_ON_NOTIFY,
            PATH_HEALTH_INFORMATION_HIP_ON_REQUEST,
            PATH_USERS_AUTH_INIT
    };

    protected static final String[] HIU_HIP_APIS = new String[]{
            PATH_HEALTH_INFORMATION_NOTIFY
    };

    protected static final String[] CM_APIS = new String[]{
            PATH_CARE_CONTEXTS_DISCOVER,
            PATH_LINK_INIT,
            PATH_LINK_CONFIRM,
            PATH_CONSENTS_ON_FETCH,
            PATH_CONSENTS_HIP_NOTIFY,
            PATH_CONSENTS_HIU_NOTIFY,
            PATH_CONSENT_REQUESTS_ON_INIT,
            PATH_PATIENTS_ON_FIND,
            PATH_HEALTH_INFORMATION_HIP_REQUEST,
            PATH_HEALTH_INFORMATION_CM_ON_REQUEST,
            PATH_USERS_AUTH_ON_INIT
    };

    protected static final String[] ALLOW_LIST_APIS = {
            PATH_CERTS,
            PATH_WELL_KNOWN_OPENID_CONFIGURATION,
            PATH_SESSIONS,
            PATH_HEARTBEAT,
            PATH_SERVICE_URLS,
            USER_SESSION
    };

    protected static final String[] INTERNAL_APIS = {
            INTERNAL_BRIDGES,
            INTERNAL_BRIDGES_BRIDGE_ID_SERVICES,
            INTERNAL_CM
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity httpSecurity,
            ReactiveAuthenticationManager authenticationManager,
            ServerSecurityContextRepository securityContextRepository) {
        return httpSecurity
                .httpBasic().disable()
                .formLogin().disable()
                .csrf().disable()
                .logout().disable()
                .authorizeExchange()
                .pathMatchers(ALLOW_LIST_APIS).permitAll().and()
                .authorizeExchange()
                .pathMatchers(INTERNAL_APIS).hasAnyRole(ADMIN.name()).and()
                .authorizeExchange()
                .pathMatchers(CM_APIS).hasAnyRole(CM.name())
                .pathMatchers(HIU_HIP_APIS).hasAnyRole(HIU.name(), HIP.name())
                .pathMatchers(HIP_APIS).hasAnyRole(HIP.name())
                .pathMatchers(HIU_APIS).hasAnyRole(HIU.name())
                .and()
                .authenticationManager(authenticationManager)
                .securityContextRepository(securityContextRepository)
                .build();
    }

    @Bean
    public ReactiveAuthenticationManager authenticationManager() {
        return new AuthenticationManager();
    }

    @Bean("centralRegistryJWKSet")
    public JWKSet jwkSet(IdentityProperties identityProperties)
            throws IOException, ParseException {
        return JWKSet.load(new URL(identityProperties.getJwkUrl()));
    }

    @Bean
    public Authenticator centralRegistryTokenVerifier(
            @Qualifier("centralRegistryJWKSet") JWKSet jwkSet) {
        return new Authenticator(jwkSet);
    }

    @Bean
    public AdminAuthenticator adminServiceTokenVerifier(
            @Qualifier("centralRegistryJWKSet") JWKSet jwkSet,
            IdentityProperties identityProperties) {
        return new AdminAuthenticator(jwkSet, identityProperties.getClientId());
    }

    @Bean
    public SecurityContextRepository contextRepository(Authenticator authenticator,
                                                       AdminAuthenticator adminAuthenticator) {
        return new SecurityContextRepository(authenticator, adminAuthenticator);
    }

    @AllArgsConstructor
    private static class SecurityContextRepository implements ServerSecurityContextRepository {
        private final Authenticator authenticator;
        private final AdminAuthenticator adminAuthenticator;

        @Override
        public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
            throw new UnsupportedOperationException("No need right now!");
        }

        @Override
        public Mono<SecurityContext> load(ServerWebExchange exchange) {
            var token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (!hasText(token)) {
                return Mono.empty();
            }
            if (isAdminAuthenticatedOnlyRequest(exchange.getRequest().getPath().toString())) {
                return checkGateway(token);
            }
            return checkCentralRegistry(token);
        }

        private Mono<SecurityContext> checkCentralRegistry(String token) {
            return authenticator.verify(token)
                    .map(caller -> {
                        var authorities = caller.getRoles()
                                .stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name().toUpperCase()))
                                .collect(toList());
                        return new UsernamePasswordAuthenticationToken(caller, token, authorities);
                    })
                    .map(SecurityContextImpl::new);
        }

        private Mono<SecurityContext> checkGateway(String token) {
            return adminAuthenticator.verify(token)
                    .map(caller -> {
                        var authorities = caller.getRoles()
                                .stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name().toUpperCase()))
                                .collect(toList());
                        return new UsernamePasswordAuthenticationToken(caller, token, authorities);
                    })
                    .map(SecurityContextImpl::new);
        }

        private boolean isAdminAuthenticatedOnlyRequest(String url) {
            AntPathMatcher antPathMatcher = new AntPathMatcher();
            return List.of(INTERNAL_APIS)
                    .stream()
                    .anyMatch(pattern -> antPathMatcher.matchStart(pattern, url));
        }
    }

    private static class AuthenticationManager implements ReactiveAuthenticationManager {
        @Override
        public Mono<Authentication> authenticate(Authentication authentication) {
            var token = authentication.getCredentials().toString();
            var auth = new UsernamePasswordAuthenticationToken(
                    authentication.getPrincipal(),
                    token,
                    new ArrayList<SimpleGrantedAuthority>());
            return Mono.just(auth);
        }
    }
}
