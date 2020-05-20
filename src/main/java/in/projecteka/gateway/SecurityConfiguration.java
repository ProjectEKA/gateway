package in.projecteka.gateway;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import in.projecteka.gateway.clients.ClientRegistryProperties;
import in.projecteka.gateway.clients.common.Authenticator;
import in.projecteka.gateway.clients.common.CentralRegistryTokenVerifier;
import in.projecteka.gateway.clients.common.UserAuthenticator;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    private static final List<Map.Entry<String, HttpMethod>> SERVICE_ONLY_URLS = new ArrayList<>() {
        {

        }
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity httpSecurity,
            ReactiveAuthenticationManager authenticationManager,
            ServerSecurityContextRepository securityContextRepository) {
        final String[] WHITELISTED_URLS = {"/**.json",
                "/ValueSet/**.json",
                "/**.html",
                "/**.js",
                "/**.yaml",
                "/**.css",
                "/**.png",
                "/health-information/fetch/**/attachments/**",
                "/sessions",
                "/config"};
        httpSecurity.authorizeExchange().pathMatchers(WHITELISTED_URLS).permitAll();
        httpSecurity.httpBasic().disable().formLogin().disable().csrf().disable().logout().disable();
        httpSecurity.authorizeExchange().pathMatchers("/**").hasAnyRole("VERIFIED");
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
    @ConditionalOnProperty(value = "hiu.loginMethod", havingValue = "jwt")
    public Authenticator userAuthenticator(byte[] sharedSecret) throws JOSEException {
        return new UserAuthenticator(sharedSecret);
    }

    @Bean
    public static byte[] sharedSecret() {
        SecureRandom random = new SecureRandom();
        byte[] sharedSecret = new byte[32];
        random.nextBytes(sharedSecret);
        return sharedSecret;
    }

    @Bean
    public SecurityContextRepository contextRepository(ReactiveAuthenticationManager manager,
                                                       CentralRegistryTokenVerifier centralRegistryTokenVerifier,
                                                       Authenticator authenticator) {
        return new SecurityContextRepository(manager, centralRegistryTokenVerifier, authenticator);
    }

    @AllArgsConstructor
    private static class SecurityContextRepository implements ServerSecurityContextRepository {
        private final ReactiveAuthenticationManager manager;
        private final CentralRegistryTokenVerifier centralRegistryTokenVerifier;
        private final Authenticator authenticator;

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

            if (isCentralRegistryAuthenticatedOnlyRequest(
                    exchange.getRequest().getPath().toString(),
                    exchange.getRequest().getMethod())) {
                return checkCentralRegistry(token);
            }
            return check(token);
        }

        private Mono<SecurityContext> check(String token) {
            return authenticator.verify(token)
                    .map(caller ->
                    {
                        var grantedAuthority = new ArrayList<SimpleGrantedAuthority>();
                        if (caller.isVerified()) {
                            grantedAuthority.add(new SimpleGrantedAuthority("ROLE_VERIFIED"));
                        }
                        caller.getRole().map(role -> grantedAuthority.add(new SimpleGrantedAuthority("ROLE_".concat(role))));
                        return new UsernamePasswordAuthenticationToken(caller, token, grantedAuthority);
                    })
                    .map(SecurityContextImpl::new);
        }

        private Mono<SecurityContext> checkCentralRegistry(String token) {
            return centralRegistryTokenVerifier.verify(token)
                    .map(caller -> new UsernamePasswordAuthenticationToken(
                            caller,
                            token,
                            new ArrayList<SimpleGrantedAuthority>()))
                    .map(SecurityContextImpl::new);
        }

        private boolean isCentralRegistryAuthenticatedOnlyRequest(String url, HttpMethod method) {
            AntPathMatcher antPathMatcher = new AntPathMatcher();
            return SERVICE_ONLY_URLS.stream()
                    .anyMatch(pattern ->
                            antPathMatcher.matchStart(pattern.getKey(), url) && pattern.getValue().equals(method));
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