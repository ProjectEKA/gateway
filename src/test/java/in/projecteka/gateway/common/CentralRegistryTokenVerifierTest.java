package in.projecteka.gateway.common;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static in.projecteka.gateway.common.Role.CM;
import static in.projecteka.gateway.common.Role.GATEWAY;
import static in.projecteka.gateway.common.Role.HIP;
import static in.projecteka.gateway.common.Role.HIU;
import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static java.lang.String.format;
import static reactor.test.StepVerifier.create;

class CentralRegistryTokenVerifierTest {

    @Test
    void returnCallerWithSupportedRolesWhenTokenHasRoles() throws JOSEException {
        var clientId = string();
        JSONArray roleValues = new JSONArray();
        var roles = List.of(HIU, GATEWAY, HIP, CM);
        var randomRole1 = string();
        var randomRole2 = string();
        roleValues.add(randomRole1);
        roleValues.add(randomRole2);
        roles.forEach(role -> roleValues.add(role.name().toUpperCase()));
        Map<String, Object> clientObj = new HashMap<>();
        clientObj.put("roles", roleValues);
        Map<String, Object> resourseAccess = new HashMap<>();
        resourseAccess.put(clientId, clientObj);
        var rsaKey = new RSAKeyGenerator(2048).keyID(string()).generate();
        var signer = new RSASSASigner(rsaKey);
        var claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://c2id.com")
                .claim("scope", string())
                .issueTime(new Date())
                .claim("clientId", clientId)
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .claim("resource_access", resourseAccess)
                .build();
        var signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID(rsaKey.getKeyID()).build(),
                claimsSet);
        signedJWT.sign(signer);
        String token = signedJWT.serialize();
        var centralRegistryTokenVerifier = new CentralRegistryTokenVerifier(new JWKSet(rsaKey.toPublicJWK()));

        Mono<Caller> verify = centralRegistryTokenVerifier.verify(format("bearer %s", token));

        create(verify).expectNext(new Caller(clientId, true, roles)).verifyComplete();
    }

    @Test
    void returnCallerWithEmptyRolesWhenTokenDoesNotHaveValidRoles() throws JOSEException {
        var clientId = string();
        JSONArray roleValues = new JSONArray();
        var randomRole1 = string();
        var randomRole2 = string();
        roleValues.add(randomRole1);
        roleValues.add(randomRole2);
        Map<String, Object> clientObj = new HashMap<>();
        clientObj.put("roles", roleValues);
        Map<String, Object> resourceAccess = new HashMap<>();
        resourceAccess.put(clientId, clientObj);
        var rsaKey = new RSAKeyGenerator(2048).keyID(string()).generate();
        var signer = new RSASSASigner(rsaKey);
        var claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://c2id.com")
                .claim("scope", string())
                .issueTime(new Date())
                .claim("clientId", clientId)
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .claim("resource_access", resourceAccess)
                .build();
        var signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID(rsaKey.getKeyID()).build(),
                claimsSet);
        signedJWT.sign(signer);
        String token = signedJWT.serialize();
        var centralRegistryTokenVerifier = new CentralRegistryTokenVerifier(new JWKSet(rsaKey.toPublicJWK()));

        Mono<Caller> verify = centralRegistryTokenVerifier.verify(format("bearer %s", token));

        create(verify).expectNext(new Caller(clientId, true, List.of())).verifyComplete();
    }

    @Test
    void returnEmptyWhenTokenDoesNotHaveResourseAccess() throws JOSEException {
        var clientId = string();
        var rsaKey = new RSAKeyGenerator(2048).keyID(string()).generate();
        var signer = new RSASSASigner(rsaKey);
        var claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://c2id.com")
                .claim("scope", string())
                .issueTime(new Date())
                .claim("clientId", clientId)
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();
        var signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID(rsaKey.getKeyID()).build(),
                claimsSet);
        signedJWT.sign(signer);
        String token = signedJWT.serialize();
        var centralRegistryTokenVerifier = new CentralRegistryTokenVerifier(new JWKSet(rsaKey.toPublicJWK()));

        Mono<Caller> verify = centralRegistryTokenVerifier.verify(format("bearer %s", token));

        create(verify).verifyComplete();
    }

    @Test
    void returnEmptyWhenTokenDoesNotHaveProperResourseAccess() throws JOSEException {
        var clientId = string();
        var resourceAccess = string();
        var rsaKey = new RSAKeyGenerator(2048).keyID(string()).generate();
        var signer = new RSASSASigner(rsaKey);
        var claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://c2id.com")
                .claim("scope", string())
                .issueTime(new Date())
                .claim("clientId", clientId)
                .claim("resource_access", resourceAccess)
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();
        var signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID(rsaKey.getKeyID()).build(),
                claimsSet);
        signedJWT.sign(signer);
        String token = signedJWT.serialize();
        var centralRegistryTokenVerifier = new CentralRegistryTokenVerifier(new JWKSet(rsaKey.toPublicJWK()));

        Mono<Caller> verify = centralRegistryTokenVerifier.verify(format("bearer %s", token));

        create(verify).verifyComplete();
    }
}