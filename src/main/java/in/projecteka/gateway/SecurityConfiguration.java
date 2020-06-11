package in.projecteka.gateway;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.clients.ClientRegistryProperties;
import in.projecteka.gateway.common.CentralRegistryTokenVerifier;
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
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;

import static in.projecteka.gateway.common.Constants.V_1_CARE_CONTEXTS_DISCOVER;
import static in.projecteka.gateway.common.Constants.V_1_CARE_CONTEXTS_ON_DISCOVER;
import static in.projecteka.gateway.common.Constants.V_1_CONSENTS_FETCH;
import static in.projecteka.gateway.common.Constants.V_1_CONSENTS_HIP_NOTIFY;
import static in.projecteka.gateway.common.Constants.V_1_CONSENTS_HIU_NOTIFY;
import static in.projecteka.gateway.common.Constants.V_1_CONSENTS_ON_FETCH;
import static in.projecteka.gateway.common.Constants.V_1_CONSENT_REQUESTS_INIT;
import static in.projecteka.gateway.common.Constants.V_1_CONSENT_REQUESTS_ON_INIT;
import static in.projecteka.gateway.common.Constants.V_1_LINKS_LINK_CONFIRM;
import static in.projecteka.gateway.common.Constants.V_1_LINKS_LINK_INIT;
import static in.projecteka.gateway.common.Constants.V_1_LINKS_LINK_ON_CONFIRM;
import static in.projecteka.gateway.common.Constants.V_1_LINKS_LINK_ON_INIT;
import static in.projecteka.gateway.common.Constants.V_1_PATIENTS_FIND;
import static in.projecteka.gateway.common.Constants.V_1_PATIENTS_ON_FIND;
import static in.projecteka.gateway.common.Role.CM;
import static in.projecteka.gateway.common.Role.HIP;
import static in.projecteka.gateway.common.Role.HIU;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.StringUtils.hasText;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    public static final String[] HIU_AP_IS = new String[]{
            V_1_CONSENT_REQUESTS_INIT,
            V_1_CONSENTS_FETCH,
            V_1_PATIENTS_FIND,
    };
    public static final String[] HIP_API_IS = new String[]{
            V_1_CARE_CONTEXTS_ON_DISCOVER,
            V_1_LINKS_LINK_ON_INIT,
            V_1_LINKS_LINK_ON_CONFIRM
    };
    public static final String[] CM_API_IS = new String[]{
            V_1_CARE_CONTEXTS_DISCOVER,
            V_1_LINKS_LINK_INIT,
            V_1_LINKS_LINK_CONFIRM,
            V_1_CONSENTS_ON_FETCH,
            V_1_CONSENTS_HIP_NOTIFY,
            V_1_CONSENTS_HIU_NOTIFY,
            V_1_CONSENT_REQUESTS_ON_INIT,
            V_1_PATIENTS_ON_FIND
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
                .pathMatchers(CM_API_IS).hasAnyRole(CM.name())
                .pathMatchers(HIP_API_IS).hasAnyRole(HIP.name())
                .pathMatchers(HIU_AP_IS).hasAnyRole(HIU.name())
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
    public JWKSet jwkSet(ClientRegistryProperties clientRegistryProperties)
            throws IOException, ParseException {
        return JWKSet.load(new URL(clientRegistryProperties.getJwkUrl()));
    }

    @Bean
    public CentralRegistryTokenVerifier centralRegistryTokenVerifier(
            @Qualifier("centralRegistryJWKSet") JWKSet jwkSet) {
        return new CentralRegistryTokenVerifier(jwkSet);
    }

    @Bean
    public SecurityContextRepository contextRepository(CentralRegistryTokenVerifier centralRegistryTokenVerifier) {
        return new SecurityContextRepository(centralRegistryTokenVerifier);
    }

    @AllArgsConstructor
    private static class SecurityContextRepository implements ServerSecurityContextRepository {
        private final CentralRegistryTokenVerifier centralRegistryTokenVerifier;

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
            return checkCentralRegistry(token);
        }

        private Mono<SecurityContext> checkCentralRegistry(String token) {
            return centralRegistryTokenVerifier.verify(token)
                    .map(caller -> {
                        var authorities = caller.getRoles()
                                .stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name().toUpperCase()))
                                .collect(toList());
                        return new UsernamePasswordAuthenticationToken(caller, token, authorities);
                    })
                    .map(SecurityContextImpl::new);
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