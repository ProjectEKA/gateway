package in.projecteka.gateway;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.clients.ClientRegistryProperties;
import in.projecteka.gateway.common.CentralRegistryTokenVerifier;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
import java.util.Map;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    private static final List<Map.Entry<String, HttpMethod>> SERVICE_ONLY_URLS = new ArrayList<>();

    static {
        SERVICE_ONLY_URLS.add(Map.entry("/v1/care-contexts/discover",HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry("/v1/care-contexts/on-discover",HttpMethod.POST));
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity httpSecurity,
            ReactiveAuthenticationManager authenticationManager,
            ServerSecurityContextRepository securityContextRepository) {
        httpSecurity.httpBasic().disable().formLogin().disable().csrf().disable().logout().disable();
        httpSecurity.authorizeExchange().pathMatchers("/**").permitAll();
        return httpSecurity
                .authenticationManager(authenticationManager)
                .securityContextRepository(securityContextRepository)
                .build();
    }

    @Bean
    public ReactiveAuthenticationManager authenticationManager() {
        return new AuthenticationManager();
    }

    @Bean("centralRegistryJWKSet")
    public JWKSet jwkSet(ClientRegistryProperties clientRegistryProperties) throws IOException, ParseException {
        return JWKSet.load(new URL(clientRegistryProperties.getJwkUrl()));
    }

    @Bean
    public CentralRegistryTokenVerifier centralRegistryTokenVerifier(@Qualifier("centralRegistryJWKSet") JWKSet jwkSet) {
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
            if (isEmpty(token)) {
                return Mono.empty();
            }
            String requestPath = exchange.getRequest().getPath().toString();
            HttpMethod requestMethod = exchange.getRequest().getMethod();

            if (isCentralRegistryAuthenticatedOnlyRequest(
                    requestPath,
                    requestMethod)) {
                return checkCentralRegistry(token);
            }
            return null; //TODO need to change
        }

        private boolean isCentralRegistryAuthenticatedOnlyRequest(String url, HttpMethod method) {
            AntPathMatcher antPathMatcher = new AntPathMatcher();
            return SERVICE_ONLY_URLS.stream()
                    .anyMatch(pattern ->
                            antPathMatcher.match(pattern.getKey(),url) && pattern.getValue().equals(method));
        }

        private Mono<SecurityContext> checkCentralRegistry(String token) {
            return centralRegistryTokenVerifier.verify(token)
                    .map(caller -> new UsernamePasswordAuthenticationToken(
                            caller,
                            token,
                            new ArrayList<SimpleGrantedAuthority>()))
                    .map(SecurityContextImpl::new);
        }

        private boolean isEmpty(String authToken) {
            return authToken == null || authToken.trim().equals("");
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